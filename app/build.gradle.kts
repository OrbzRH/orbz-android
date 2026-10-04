import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// Build-time settings come from local.properties, a -P property or the environment, never from the source tree:
//   orbz.wcProjectId     the WalletConnect (Reown) project id, public by design, like a site key
//   orbz.apiBase         debug builds only: where the console runs (default http://localhost:3181, reached from
//                        the emulator through `adb reverse tcp:3181 tcp:3181`)
//   orbz.keystore, orbz.keystorePassword, orbz.keyAlias, orbz.keyPassword   release signing
val local = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun setting(name: String, default: String = ""): String =
    (project.findProperty(name) as String?) ?: local.getProperty(name) ?: System.getenv(name.uppercase().replace('.', '_')) ?: default

android {
    namespace = "app.orbz.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.orbz.android"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "WC_PROJECT_ID", "\"${setting("orbz.wcProjectId")}\"")
        buildConfigField("String", "API_BASE", "\"https://use.orbz.app\"")
        buildConfigField("String", "SITE", "\"https://orbz.app\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["pk"] = setting("orbz.testPk")
        testInstrumentationRunnerArguments["key"] = setting("orbz.testKey")
        // phones only; the x86 builds of the wallet SDK's native code are for emulators and would double the APK
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
    }

    signingConfigs {
        val ks = setting("orbz.keystore")
        if (ks.isNotBlank()) {
            create("release") {
                storeFile = file(ks)
                storePassword = setting("orbz.keystorePassword")
                keyAlias = setting("orbz.keyAlias", "orbz")
                keyPassword = setting("orbz.keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            buildConfigField("String", "API_BASE", "\"${setting("orbz.apiBase", "http://localhost:3181")}\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging {
        resources.excludes += setOf("META-INF/LICENSE*", "META-INF/NOTICE*", "META-INF/DEPENDENCIES", "META-INF/INDEX.LIST", "META-INF/*.kotlin_module", "META-INF/versions/9/OSGI-INF/MANIFEST.MF")
    }
    testOptions {
        animationsDisabled = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    // AppKit's sheet is a Material (1) bottom sheet driven by navigation
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material:material-navigation")
    // the small core icon set; the extended set is 10 MB for a handful of glyphs
    implementation("androidx.compose.material:material-icons-core:1.7.8")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.navigation:navigation-compose:2.10.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("com.squareup.okhttp3:okhttp:5.5.0")

    implementation(platform("com.reown:android-bom:1.6.17"))
    implementation("com.reown:android-core")
    implementation("com.reown:appkit")

    // Debug builds only: a local signer so the whole sign-in flow can run on an emulator without a wallet app.
    debugImplementation("org.bouncycastle:bcprov-jdk18on:1.86")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test:rules:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.09.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
