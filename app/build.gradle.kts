import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("com.google.devtools.ksp") version AndroidConfig.kspVersion
    id("dev.rikka.tools.refine") version AndroidConfig.rikkaRefineVersion
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0"
}

android {

    val SUPPORTED_ABIS = setOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
    compileSdk = AndroidConfig.compileSdk
    project.setProperty("archivesBaseName", "Cafetone-v${AndroidConfig.versionName}")

    defaultConfig {
        targetSdk = AndroidConfig.targetSdk
        versionCode = AndroidConfig.versionCode
        versionName = AndroidConfig.versionName
        applicationId = "com.cafetone.dsp"
        minSdk = AndroidConfig.minSdk

        manifestPlaceholders["label"] = "Cafetone"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "COMMIT_COUNT", "\"${getCommitCount()}\"")
        buildConfigField("String", "COMMIT_SHA", "\"${getGitSha()}\"")
        buildConfigField("String", "BUILD_TIME", "\"${getBuildTime()}\"")
        buildConfigField("boolean", "PREVIEW", "false")
        buildConfigField("boolean", "PLUGIN", "false")
        buildConfigField("boolean", "ROOTLESS", "true")
        buildConfigField("boolean", "FOSS_ONLY", "false")

        externalNativeBuild {
            cmake {
                arguments.addAll(listOf("-DANDROID_ARM_NEON=ON"))
                cFlags.add("-std=gnu11 -Wno-incompatible-pointer-types -Wno-implicit-int -Wno-implicit-function-declaration")
            }
        }

        ndk {
            abiFilters += SUPPORTED_ABIS
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-${getCommitCount()}"
            manifestPlaceholders["crashlyticsCollectionEnabled"] = "false"
        }
        getByName("release") {
            manifestPlaceholders += mapOf("crashlyticsCollectionEnabled" to "true")
            configure<CrashlyticsExtension> {
                nativeSymbolUploadEnabled = true
                mappingFileUploadEnabled = false
            }

            //proguardFiles("proguard-android-optimize.txt", "proguard-rules.pro")
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    sourceSets {
        // Use different app icon for non-release builds
        getByName("debug").res.srcDirs("src/debug/res")
    }

    // Export multiple CPU architecture split apks
    splits {
        abi {
            isEnable = true
            reset()
            include(*SUPPORTED_ABIS.toTypedArray())
            isUniversalApk = true
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
        disable += "ObsoleteSdkInt"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        // Disable unused features
        aidl = false
        renderScript = false
        shaders = false
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    namespace = "com.cafetone.dsp"
}

dependencies {
    // Kotlin extensions
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.0.20")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")

    // AndroidX
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.4")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.4")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.databinding:databinding-runtime:8.7.3")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("androidx.mediarouter:mediarouter:1.7.0")

    // Material
    implementation("com.google.android.material:material:1.9.0")

    // Dependency injection
    implementation("io.insert-koin:koin-android:3.3.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")

    // Firebase
    "fullImplementation"(platform("com.google.firebase:firebase-bom:33.7.0"))
    "fullImplementation"("com.google.firebase:firebase-analytics-ktx")
    "fullImplementation"("com.google.firebase:firebase-crashlytics-ktx")
    "fullImplementation"("com.google.firebase:firebase-crashlytics-ndk")

    // Web API client
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0")

    // Logging
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("com.github.bastienpaulfr:Treessence:1.0.0")

    // IO
    implementation("org.kamranzafar:jtar:2.3")
    implementation("com.squareup.okio:okio:3.6.0")

    // Room databases
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")

    // Script editor
    implementation(project(":codeview"))

    // Shizuku
    implementation("dev.rikka.shizuku:api:${AndroidConfig.shizukuVersion}")
    implementation("dev.rikka.shizuku:provider:${AndroidConfig.shizukuVersion}")

    // Used for backup file access
    implementation("com.github.tachiyomiorg:unifile:17bec43")

    // Root APIs
    "rootImplementation"("com.github.topjohnwu.libsu:core:5.0.4")

    // Hidden APIs
    implementation("dev.rikka.tools.refine:runtime:${AndroidConfig.rikkaRefineVersion}")
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:4.3")
    compileOnly(project(":hidden-api-refined"))
    implementation(project(":hidden-api-impl"))

    // Debug utilities
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.10")
    debugImplementation("com.plutolib:pluto:2.0.9")
    "previewImplementation"("com.plutolib:pluto-no-op:2.0.9")
    releaseImplementation("com.plutolib:pluto-no-op:2.0.9")
    debugImplementation("com.plutolib.plugins:bundle-core:2.0.9")
    "previewImplementation"("com.plutolib.plugins:bundle-core-no-op:2.0.9")
    releaseImplementation("com.plutolib.plugins:bundle-core-no-op:2.0.9")

    // Unit tests
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}