import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/** Couverture des lignes du domaine en dessous de laquelle `koverVerify` échoue. */
val coverageMinPercent = 80

// Logique métier en Kotlin pur, sans Android ni Hilt : se teste sur n'importe quelle JVM,
// y compris dans une session cloud sans SDK Android (`./gradlew :core:domain:test`).
plugins {
    alias(libs.plugins.kotlin.jvm)
    // Couverture mesurée et seuil : `koverVerify`.
    alias(libs.plugins.kover)
    // Complexité et code suspect ; le formatage reste à ktlint (spotless).
    alias(libs.plugins.detekt)
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

kover {
    reports {
        filters {
            excludes {
                // Données sans logique (classes de données, énumérations) : leur couverture ne dit rien.
                packages("com.bastiensabos.betterhusband.domain.model")
            }
        }
        verify {
            rule("Le domaine reste couvert par ses tests") {
                minBound(coverageMinPercent)
            }
        }
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
}
