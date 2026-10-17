buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        // AGP 9 má vstavaný Kotlin; verziu KGP zarovnávame s Compose compiler pluginom.
        classpath(libs.kotlin.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
