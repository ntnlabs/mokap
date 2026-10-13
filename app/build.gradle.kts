import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Release podpisovanie: hodnoty ber z env premenných (CI) alebo z keystore.properties
// v koreni repa (lokálne). Oba zdroje sú mimo gitu – pozri README.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.isFile) file.inputStream().use { load(it) }
}

fun signingValue(envName: String, propName: String): String? =
    providers.environmentVariable(envName).orNull?.takeIf { it.isNotBlank() }
        ?: keystoreProperties.getProperty(propName)?.takeIf { it.isNotBlank() }

val releaseStoreFile = signingValue("MOKAP_KEYSTORE_FILE", "storeFile")

android {
    namespace = "sk.ntnlabs.kyberia"
    compileSdk = 37

    defaultConfig {
        applicationId = "sk.ntnlabs.kyberia"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "START_URL", "\"https://kyberia.sk/\"")
    }

    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile)
                storePassword = signingValue("MOKAP_KEYSTORE_PASSWORD", "storePassword")
                keyAlias = signingValue("MOKAP_KEY_ALIAS", "keyAlias")
                keyPassword = signingValue("MOKAP_KEY_PASSWORD", "keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Bez keystore vznikne nepodpísaný release build.
            signingConfig = signingConfigs.findByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.webkit)
    implementation(libs.androidx.swiperefreshlayout)

    testImplementation(libs.junit)
}
