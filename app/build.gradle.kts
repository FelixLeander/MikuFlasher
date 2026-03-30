plugins {
    id("com.android.application")
    id("de.undercouch.download")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.google.mediapipe.examples.poselandmarker"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.google.mediapipe.examples.poselandmarker"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
}

// import DownloadMPTasks task
//extra.set("ASSET_DIR", "$projectDir/src/main/assets")
//apply(from = "download_tasks.gradle.kts")

dependencies {

    // Kotlin lang
    implementation("androidx.core:core-ktx:1.18.0")

    // App compat and UI things
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.fragment:fragment-ktx:1.8.9")

    // Navigation library
    val navVersion = "2.9.7"
    implementation("androidx.navigation:navigation-fragment-ktx:$navVersion")
    implementation("androidx.navigation:navigation-ui-ktx:$navVersion")

    // CameraX core library
    val cameraxVersion = "1.6.0"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // WindowManager
    implementation("androidx.window:window:1.5.1")

    // Unit testing
    testImplementation("junit:junit:4.13.2")

    // Instrumented testing
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")

    // Jetpack Compose
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui:1.10.6")
    implementation("androidx.compose.material:material:1.10.6")
    implementation("androidx.compose.ui:ui-tooling-preview:1.10.6")
    debugImplementation("androidx.compose.ui:ui-tooling:1.10.6")

    // MediaPipe Library
    implementation("com.google.mediapipe:tasks-vision:0.20230731")
}