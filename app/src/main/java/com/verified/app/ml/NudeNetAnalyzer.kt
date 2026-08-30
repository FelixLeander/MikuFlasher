package com.verified.app.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import androidx.core.graphics.createBitmap

// ── Constants ─────────────────────────────────────────────────────────────────

private const val MODEL_FILE        = "nudeNet_320n_w8a32.tflite"
private const val INPUT_SIZE        = 320
private const val NUM_ANCHORS       = 2100
private const val NUM_CLASSES       = 18
private const val CONF_THRESHOLD    = 0.25f
private const val NMS_IOU_THRESHOLD = 0.45f

// Class index → human-readable name, in the order the model was trained
val NUDENET_CLASS_NAMES = arrayOf(
    "FEMALE_GENITALIA_COVERED",  // 0
    "FACE_FEMALE",               // 1
    "BUTTOCKS_EXPOSED",          // 2
    "FEMALE_BREAST_EXPOSED",     // 3  ← primary target
    "FEMALE_GENITALIA_EXPOSED",  // 4
    "MALE_BREAST_EXPOSED",       // 5
    "ANUS_EXPOSED",              // 6
    "FEET_EXPOSED",              // 7
    "BELLY_COVERED",             // 8
    "FEET_COVERED",              // 9
    "ARMPITS_COVERED",           // 10
    "ARMPITS_EXPOSED",           // 11
    "FACE_MALE",                 // 12
    "BELLY_EXPOSED",             // 13
    "MALE_GENITALIA_EXPOSED",    // 14
    "ANUS_COVERED",              // 15
    "FEMALE_BREAST_COVERED",     // 16 ← primary target
    "BUTTOCKS_COVERED",          // 17
)

private const val CLASS_BREAST_EXPOSED = 3
private const val CLASS_BREAST_COVERED = 16

// ── Public result types ───────────────────────────────────────────────────────

enum class BreastState { NONE, COVERED, EXPOSED, BOTH }

data class NudeDetection(
    val classIndex: Int,
    val className: String,
    val confidence: Float,
    /** All coordinates normalised to [0, 1] relative to the original frame. */
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
)

data class NudeNetResult(
    val detections: List<NudeDetection>,
    val breastState: BreastState,
    /** Highest confidence score among any breast detection. 0 if none found. */
    val breastConfidence: Float,
)

// ── Analyzer ─────────────────────────────────────────────────────────────────

class NudeNetAnalyzer(context: Context) {

    private val interpreter = Interpreter(
        loadModelBuffer(context),
        Interpreter.Options().apply { numThreads = 2 },
    )

    // Reusable buffers — not thread-safe; call only from a single analysis thread
    private val inputBuffer = ByteBuffer
        .allocateDirect(3 * INPUT_SIZE * INPUT_SIZE * Float.SIZE_BYTES)
        .apply { order(ByteOrder.nativeOrder()) }

    private val outputBuffer = Array(1) { Array(22) { FloatArray(NUM_ANCHORS) } }
    private val pixelScratch = IntArray(INPUT_SIZE * INPUT_SIZE)

    // Pre-allocated 320×320 bitmap + Canvas so preprocess() never allocates
    private val scaledBitmap = createBitmap(INPUT_SIZE, INPUT_SIZE)
    private val scalingCanvas = Canvas(scaledBitmap)
    private val scalingPaint  = Paint(Paint.FILTER_BITMAP_FLAG)
    private val scalingDstRect = RectF(0f, 0f, INPUT_SIZE.toFloat(), INPUT_SIZE.toFloat())

    /** Run inference on a [Bitmap] of any size. Returns detected classes + breast state. */
    fun analyze(bitmap: Bitmap): NudeNetResult {
        preprocess(bitmap)
        interpreter.run(inputBuffer, outputBuffer)
        return postprocess(outputBuffer[0])
    }

    fun close() {
        interpreter.close()
        scaledBitmap.recycle()
    }

    // ── Preprocessing ─────────────────────────────────────────────────────────

    private fun preprocess(source: Bitmap) {
        // Draw source into the pre-allocated 320×320 bitmap — zero allocations
        scalingCanvas.drawBitmap(source, null, scalingDstRect, scalingPaint)
        scaledBitmap.getPixels(pixelScratch, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        inputBuffer.rewind()
        // NCHW layout: full R plane, then G, then B, each normalised to [0, 1]
        for (px in pixelScratch) inputBuffer.putFloat(((px shr 16) and 0xFF) / 255f) // R
        for (px in pixelScratch) inputBuffer.putFloat(((px shr 8)  and 0xFF) / 255f) // G
        for (px in pixelScratch) inputBuffer.putFloat((px           and 0xFF) / 255f) // B
        inputBuffer.rewind()
    }

    // ── Post-processing ───────────────────────────────────────────────────────

    private fun postprocess(output: Array<FloatArray>): NudeNetResult {
        // output[row][anchor]: rows 0-3 = cx,cy,w,h; rows 4-21 = 18 class scores

        val candidates = ArrayList<RawBox>(64)

        for (j in 0 until NUM_ANCHORS) {
            var maxScore = 0f
            var maxClass = 0
            for (c in 0 until NUM_CLASSES) {
                val s = output[4 + c][j]
                if (s > maxScore) { maxScore = s; maxClass = c }
            }
            if (maxScore < CONF_THRESHOLD) continue

            val cx = output[0][j]
            val cy = output[1][j]
            val hw = output[2][j] / 2f
            val hh = output[3][j] / 2f

            candidates += RawBox(
                x1 = (cx - hw).coerceIn(0f, 1f),
                y1 = (cy - hh).coerceIn(0f, 1f),
                x2 = (cx + hw).coerceIn(0f, 1f),
                y2 = (cy + hh).coerceIn(0f, 1f),
                confidence = maxScore,
                classIndex = maxClass,
            )
        }

        val kept = nms(candidates)

        val detections = kept.map { b ->
            NudeDetection(
                classIndex  = b.classIndex,
                className   = NUDENET_CLASS_NAMES[b.classIndex],
                confidence  = b.confidence,
                x1 = b.x1, y1 = b.y1, x2 = b.x2, y2 = b.y2,
            )
        }

        val hasExposed = detections.any { it.classIndex == CLASS_BREAST_EXPOSED }
        val hasCovered = detections.any { it.classIndex == CLASS_BREAST_COVERED }
        val breastState = when {
            hasExposed && hasCovered -> BreastState.BOTH
            hasExposed               -> BreastState.EXPOSED
            hasCovered               -> BreastState.COVERED
            else                     -> BreastState.NONE
        }
        val breastConf = detections
            .filter { it.classIndex == CLASS_BREAST_EXPOSED || it.classIndex == CLASS_BREAST_COVERED }
            .maxOfOrNull { it.confidence } ?: 0f

        return NudeNetResult(detections, breastState, breastConf)
    }

    // ── NMS ───────────────────────────────────────────────────────────────────

    private data class RawBox(
        val x1: Float, val y1: Float,
        val x2: Float, val y2: Float,
        val confidence: Float,
        val classIndex: Int,
    )

    private fun nms(boxes: List<RawBox>): List<RawBox> {
        if (boxes.isEmpty()) return emptyList()
        val sorted = boxes.sortedByDescending { it.confidence }
        val suppressed = BooleanArray(sorted.size)
        val kept = ArrayList<RawBox>()

        for (i in sorted.indices) {
            if (suppressed[i]) continue
            kept += sorted[i]
            for (j in i + 1 until sorted.size) {
                if (!suppressed[j] && iou(sorted[i], sorted[j]) > NMS_IOU_THRESHOLD) {
                    suppressed[j] = true
                }
            }
        }
        return kept
    }

    private fun iou(a: RawBox, b: RawBox): Float {
        val ix1 = maxOf(a.x1, b.x1)
        val iy1 = maxOf(a.y1, b.y1)
        val ix2 = minOf(a.x2, b.x2)
        val iy2 = minOf(a.y2, b.y2)
        val inter = maxOf(0f, ix2 - ix1) * maxOf(0f, iy2 - iy1)
        if (inter == 0f) return 0f
        val aArea = (a.x2 - a.x1) * (a.y2 - a.y1)
        val bArea = (b.x2 - b.x1) * (b.y2 - b.y1)
        return inter / (aArea + bArea - inter + 1e-6f)
    }

    // ── Model loading ─────────────────────────────────────────────────────────

    companion object {
        private fun loadModelBuffer(context: Context): ByteBuffer {
            val afd = context.assets.openFd(MODEL_FILE)
            return FileInputStream(afd.fileDescriptor).use { fis ->
                fis.channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    afd.startOffset,
                    afd.declaredLength,
                )
            }
        }
    }
}
