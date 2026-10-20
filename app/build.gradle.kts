import java.util.Locale
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
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

// Verzia major.minor.build: major, minor a kódové meno sú ručne vo version.properties,
// build je číslo behu CI – dostane ho len job apk (MOKAP_BUILD_NUMBER), inde je 0,
// aby sa BuildConfig pri bežných kontrolách nemenil a fungovala build cache.
val versionProperties = Properties().apply {
    load(providers.fileContents(rootProject.layout.projectDirectory.file("version.properties")).asText.get().reader())
}
val versionMajor = versionProperties.getProperty("major").trim().toInt()
val versionMinor = versionProperties.getProperty("minor").trim().toInt()
val versionBuild = providers.environmentVariable("MOKAP_BUILD_NUMBER").orNull?.toIntOrNull() ?: 0
val versionCodename = versionProperties.getProperty("codename").trim()
val appVersion = String.format(Locale.ROOT, "%d.%d.%04d", versionMajor, versionMinor, versionBuild)

fun javaString(value: String): String = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "sk.ntnlabs.kyberia"
    compileSdk = 37

    defaultConfig {
        applicationId = "sk.ntnlabs.kyberia"
        minSdk = 29
        targetSdk = 36
        // Musí stále rásť, inak Android aktualizáciu odmietne.
        versionCode = versionMajor * 10_000_000 + versionMinor * 100_000 + versionBuild
        versionName = appVersion
        // Bez prípony „-debug“ – tú pridáva len debug build do versionName.
        buildConfigField("String", "APP_VERSION", javaString(appVersion))
        buildConfigField("String", "CODENAME", javaString(versionCodename))
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
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.okhttp)
    implementation(libs.jsoup)
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.coil.network.okhttp)

    testImplementation(libs.junit)
}
