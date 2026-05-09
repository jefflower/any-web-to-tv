plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.jefflower.anywebtotv"
    // GeckoView 138's transitive androidx.core 1.15 demands compileSdk ≥ 35.
    // Locally installed: android-34 and android-36 (no 35) — using 36 with AGP 8.7
    // produces a benign "max recommended" warning but works. targetSdk stays at 34
    // to keep runtime behavior unchanged for users.
    compileSdk = 36

    defaultConfig {
        applicationId = "com.jefflower.anywebtotv"
        minSdk = 23
        targetSdk = 34
        versionCode = 5
        versionName = "0.2.0-gecko"
        // ABI selection happens via `splits.abi` below — produces one APK per ABI
        // (~30-40MB each with GeckoView) instead of a 200MB+ universal one.
        // We don't use `abiFilters` here because AGP rejects having both at once.
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    // Produce one APK per ABI so we can sideload only the slice the TV needs (~30-40MB each
    // for the GeckoView build). Universal APK disabled to keep CI artifacts small.
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk = false
        }
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
    }

    // GeckoView's R8/ProGuard rules ship in its AAR; this is a belt-and-suspenders keep
    // for cases where R8 misses Gecko's JNI entry points.
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.leanback:leanback:1.0.0")
    implementation("androidx.webkit:webkit:1.12.1")
    implementation("com.google.android.material:material:1.12.0")

    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // GeckoView (Mozilla Firefox engine, full ES2022 / top-level await support).
    // Replaces system android.webkit.WebView so old TV ROMs (Mi TV stuck on Chrome 83)
    // can still run modern SPAs / module-federation builds.
    //
    // 138.x is the highest stable that lines up with our toolchain:
    //   - >=150 transitively requires androidx.core 1.18 → AGP 8.9.1+ / compileSdk 36
    //   - >=140 transitively requires Kotlin stdlib 2.2 (we are on 2.0)
    //
    // Latest list: https://maven.mozilla.org/maven2/org/mozilla/geckoview/geckoview/maven-metadata.xml
    implementation("org.mozilla.geckoview:geckoview:138.0.20250517143237")
}
