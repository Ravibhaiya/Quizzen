import java.security.MessageDigest

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.ravibhaiya.quizzen"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ravibhaiya.quizzen"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        // The app is English only; drop the translations that Material and AndroidX libraries bundle (smaller APK).
        // Add a language here when the app gets translated. (AGP 8.8+ renames this to `androidResources.localeFilters`.)
        resourceConfigurations += listOf("en")

        fun String.isPlaceholderKey(): Boolean {
            if (this == "YOUR_GEMINI_API_KEY") return true
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
            return digest == "1238bfb6c827dbce27869dda4886e148174fd475baaaac03ff41242e45dc0f7b"
        }
        val envFile = rootProject.file(".env")
        val fileKey = if (envFile.exists()) {
            envFile.readLines()
                .map { it.trim() }
                .firstOrNull { it.startsWith("GEMINI_API_KEY=") }
                ?.substringAfter("GEMINI_API_KEY=")
                ?.trim()
                ?.removeSurrounding("\"")
                ?.takeIf { it.isNotBlank() && !it.isPlaceholderKey() }
        } else {
            null
        }
        val envVarKey = System.getenv("GEMINI_API_KEY")
            ?.trim()
            ?.removeSurrounding("\"")
            ?.takeIf { it.isNotBlank() && !it.isPlaceholderKey() }
        val rawGeminiKey = (fileKey ?: envVarKey ?: "")
            .trim()
            .removeSurrounding("\"")
        val escapedGeminiKey = rawGeminiKey.replace("\\", "\\\\").replace("\"", "\\\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"$escapedGeminiKey\"")
    }

    // A shared debug key (`debug.keystore` in the project root, never committed) keeps debug APKs signed the same on every
    // machine. When the file is absent (CI, fresh clones) the debug build falls back to Android's automatically generated
    // debug key instead of failing with "Keystore file not found".
    val sharedDebugKeystore = file("${rootDir}/debug.keystore")

    signingConfigs {
        create("debugConfig") {
            storeFile = sharedDebugKeystore
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName(
                if (sharedDebugKeystore.exists()) "debugConfig" else "debug",
            )
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // For on-device performance checks only: `-PsignReleaseWithDebugKey` signs the optimized release build with
            // the debug key so it can be installed directly. Without the flag the release build stays unsigned; Play Store
            // builds must use the real upload key instead.
            if (providers.gradleProperty("signReleaseWithDebugKey").isPresent) {
                signingConfig = signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.kotlinx.coroutines.play.services)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.matching { it.name.startsWith("process") && it.name.endsWith("GoogleServices") }.configureEach {
    onlyIf { file("google-services.json").exists() }
}

