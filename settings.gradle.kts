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
        google()
        mavenCentral()
    }
}

rootProject.name = "MenuPlanner"

include(":app")
include(":core:domain")
include(":core:data")
include(":core:ui")
include(":core:cloud")
include(":feature:onboarding")
include(":feature:dishes")
include(":feature:calendar")
include(":feature:settings")
