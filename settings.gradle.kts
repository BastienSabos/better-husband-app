pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "better-husband"
include(":core:domain")
include(":architecture")

// Sans SDK Android ni accès à dl.google.com (session Claude Code dans le cloud) : `-PjvmOnly=true`, ou
// ORG_GRADLE_PROJECT_jvmOnly=true, ne garde que les modules Kotlin purs. :app se construit alors dans la CI.
if (providers.gradleProperty("jvmOnly").orNull != "true") {
    include(":app")
}
