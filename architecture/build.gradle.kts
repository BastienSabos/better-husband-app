import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Règles d'architecture et de conventions (AGENTS.md) écrites en tests Konsist : elles lisent les sources de tout
// le dépôt sans les compiler, donc tournent sur une JVM nue, y compris dans une session cloud sans SDK Android.
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
    testImplementation(libs.konsist)
}

tasks.test {
    // Konsist lit les fichiers du dépôt : toute source modifiée relance les règles.
    inputs
        .files(
            fileTree(rootDir) {
                include("**/src/**/*.kt")
                include("**/src/main/AndroidManifest.xml")
                exclude("**/build/**")
            },
        ).withPathSensitivity(PathSensitivity.RELATIVE)
}
