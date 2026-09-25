plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.pickupcode"
    // Material 3 Expressive (1.4.0-alpha) 依赖要求 compileSdk >= 35；本机已装 android-35 平台
    compileSdk = 35
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.example.pickupcode"
        // 最低支持 Android 8.0 (API 26)
        minSdk = 26
        targetSdk = 34
        versionCode = 11
        versionName = "1.0.10"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // 新手项目：不启用代码混淆，保证直接可运行
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // 开启 ViewBinding（旧 XML 布局退役后无引用，保留无害）
    buildFeatures {
        viewBinding = true
        // 启用 Jetpack Compose
        compose = true
        // 生成 BuildConfig（用于显示版本号）
        buildConfig = true
    }
}

// APK 输出命名为 app-<versionName>.apk（如 app-1.0.1.apk），每次版本号递增自动跟随
android {
    applicationVariants.all {
        outputs.all {
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl)
                .outputFileName = "app-${versionName}.apk"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.activity.ktx)

    // Google ML Kit 中文文本识别（本地离线模型，无需联网权限）
    implementation(libs.mlkit.text.recognition.chinese)

    // CameraX 相机实时预览 + 逐帧分析（实时扫描页用；1.3.4 兼容 compileSdk 34 / AGP 8.7.3）
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    // ===== Jetpack Compose + Material 3 Expressive =====
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
