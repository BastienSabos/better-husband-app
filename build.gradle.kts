plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.spotless)
}

// Formatage unique pour tout le repo : `./gradlew spotlessApply` corrige, `spotlessCheck` vérifie (CI).
spotless {
    val ktlintVersion = libs.versions.ktlint.get()
    val composeRules = libs.compose.rules.ktlint.get().toString()
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**")
        ktlint(ktlintVersion).customRuleSets(listOf(composeRules))
    }
    kotlinGradle {
        target("**/*.kts")
        targetExclude("**/build/**")
        ktlint(ktlintVersion)
    }
    format("xml") {
        target("**/*.xml")
        targetExclude("**/build/**", ".idea/**")
        trimTrailingWhitespace()
        endWithNewline()
    }
}
