// app/build.gradle.kts by Daniele Lolli (UncleDan) feat. Claude AI - Release 1.0.0 - 2026-10-24 14-42-28
import java.net.URI

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Firma release: attiva solo se le variabili d'ambiente sono presenti (GitHub Actions o locale)
val keystorePath: String? = System.getenv("PDM_KEYSTORE_PATH")
val hasReleaseKeystore = !keystorePath.isNullOrBlank() && file(keystorePath).exists()

android {
    namespace = "it.uncledan.pdmeasure"
    compileSdk = 35

    defaultConfig {
        applicationId = "it.uncledan.pdmeasure"
        minSdk = 24
        targetSdk = 35
        // VERSIONE: valori letterali (leggibili staticamente, es. da F-Droid).
        // Regola: versionCode = major*10000 + minor*100 + patch (verificata in build).
        versionCode = 10000
        versionName = "1.0.0"
        manifestPlaceholders["appLabel"] = "PD Measure"
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = file(keystorePath!!)
                storePassword = System.getenv("PDM_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("PDM_KEY_ALIAS")
                keyPassword = System.getenv("PDM_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            manifestPlaceholders["appLabel"] = "PD Measure Debug"
        }
        getByName("release") {
            // R8 disattivato: MediaPipe usa riflessione/protobuf, si evita il rischio di crash
            isMinifyEnabled = false
            if (hasReleaseKeystore) signingConfig = signingConfigs.getByName("release")
        }
        create("beta") {
            initWith(getByName("release"))
            applicationIdSuffix = ".beta"
            versionNameSuffix = "-beta"
            manifestPlaceholders["appLabel"] = "PD Measure Beta"
            signingConfig = if (hasReleaseKeystore) signingConfigs.getByName("release")
                            else signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Il modello va letto non compresso dagli asset
    androidResources { noCompress += "task" }

    lint { checkReleaseBuilds = false }
}

// --- Controllo coerenza versionName / versionCode ---
run {
    val vn = android.defaultConfig.versionName ?: error("versionName mancante")
    val vc = android.defaultConfig.versionCode ?: error("versionCode mancante")
    val p = vn.split(".").map { it.toIntOrNull() ?: error("versionName non numerico: $vn") }
    require(p.size in 2..3) { "versionName deve essere major.minor[.patch]: $vn" }
    val expected = p[0] * 10000 + p[1] * 100 + p.getOrElse(2) { 0 }
    check(vc == expected) { "versionCode ($vc) incoerente con versionName ($vn): atteso $expected" }
}

// --- Download automatico del modello MediaPipe Face Landmarker (con iride) ---
val faceModelUrl =
    "https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/face_landmarker.task"
val faceModelFile = file("src/main/assets/face_landmarker.task")

val downloadFaceModel by tasks.registering {
    description = "Scarica face_landmarker.task negli asset se assente"
    outputs.file(faceModelFile)
    doLast {
        if (!faceModelFile.exists() || faceModelFile.length() < 1_000_000L) {
            faceModelFile.parentFile.mkdirs()
            logger.lifecycle("Download modello MediaPipe: $faceModelUrl")
            URI(faceModelUrl).toURL().openStream().use { input ->
                faceModelFile.outputStream().use { input.copyTo(it) }
            }
        }
    }
}
tasks.named("preBuild") { dependsOn(downloadFaceModel) }

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    val camerax = "1.4.1"
    implementation("androidx.camera:camera-core:$camerax")
    implementation("androidx.camera:camera-camera2:$camerax")
    implementation("androidx.camera:camera-lifecycle:$camerax")

    implementation("com.google.mediapipe:tasks-vision:0.10.14")

    testImplementation("junit:junit:4.13.2")
}
