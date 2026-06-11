import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}

fun signingValue(name: String): String? =
    providers.environmentVariable(name).orNull
        ?: localProps.getProperty(name)

android {
    namespace = "com.carldong.fifa.worldcup2026"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.carldong.fifa.worldcup2026"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "1.2"
        buildConfigField(
            "String",
            "RESULTS_CSV_URL",
            "\"https://fifa-world-cup-2026-server.vercel.app/data/results.csv\""
        )
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file(signingValue("FIFA_STORE_FILE") ?: "fifa-release.jks")
            storePassword = signingValue("FIFA_STORE_PASSWORD")
            keyAlias = signingValue("FIFA_KEY_ALIAS")
            keyPassword = signingValue("FIFA_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.serialization.json)

    debugImplementation(libs.androidx.ui.tooling)
}
