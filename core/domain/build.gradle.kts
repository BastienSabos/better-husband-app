import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Logique métier en Kotlin pur, sans Android ni Hilt : se teste sur n'importe quelle JVM,
// y compris dans une session cloud sans SDK Android (`./gradlew :core:domain:test`).
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
