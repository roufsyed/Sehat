import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
}

// Read signing credentials from local.properties (never hardcode paths or passwords)
val localProps = Properties().also { props ->
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use(props::load)
}

android {
    namespace = "com.rouf.saht"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rouf.saht"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val kFile     = localProps.getProperty("KEYSTORE_FILE")
            val kPass     = localProps.getProperty("KEYSTORE_PASSWORD")
            val kAlias    = localProps.getProperty("KEY_ALIAS")
            val kKeyPass  = localProps.getProperty("KEY_PASSWORD")
            if (!kFile.isNullOrBlank() && !kPass.isNullOrBlank()
                && !kAlias.isNullOrBlank() && !kKeyPass.isNullOrBlank()
            ) {
                storeFile     = file(kFile)
                storePassword = kPass
                keyAlias      = kAlias
                keyPassword   = kKeyPass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled   = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseSigningConfig = signingConfigs.getByName("release")
            if (releaseSigningConfig.storeFile != null) {
                signingConfig = releaseSigningConfig
            }
        }
        debug {
            isMinifyEnabled   = false
            isShrinkResources = false
            applicationIdSuffix = ".debug"
            versionNameSuffix   = "-debug"
        }
    }

    // Output filename: Sehat_<versionName>_<buildType>.apk
    applicationVariants.all {
        val variant = this
        outputs.all {
            val output = this as? com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output?.outputFileName = "Sehat_${variant.versionName}_${variant.buildType.name}.apk"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
    hilt {
        enableAggregatingTask = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.activity)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Hilt — single source of truth for version
    implementation("com.google.dagger:hilt-android:2.53.1")
    kapt("com.google.dagger:hilt-compiler:2.53.1")
    implementation("androidx.hilt:hilt-navigation-fragment:1.2.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    // CameraX
    val cameraxVersion = "1.4.1"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // Database
    implementation("io.github.pilgr:paperdb:2.7.2")

    // Images
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Heart rate
    implementation(files("libs/heartrateometer-release.aar"))

    // Charts
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Gson
    implementation("com.google.code.gson:gson:2.12.1")

    // ExoPlayer / Media3
    implementation("androidx.media3:media3-exoplayer:1.5.1")

    // ViewPager2 (onboarding)
    implementation("androidx.viewpager2:viewpager2:1.1.0")
}
