// Les plugins Android et Kotlin ne sont pas déclarés ici mais dans chaque module : chargés à la racine, ils ne voient
// pas AGP 9 (chargé dans :app) et cassent sa configuration, et le build reste configurable sans maven.google.com
// quand :app est exclu (-PjvmOnly=true).
plugins {
    alias(libs.plugins.spotless)
}

// Formatage unique pour tout le repo : `./gradlew spotlessApply` corrige, `spotlessCheck` vérifie (CI).
spotless {
    val ktlintVersion = libs.versions.ktlint.get()
    val composeRules =
        libs.compose.rules.ktlint
            .get()
            .toString()
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
