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

rootProject.name = "StockPulse"

include(":app")
include(":core:network")
include(":core:database")
include(":core:designsystem")
include(":core:model")
include(":feature:dashboard")
include(":feature:stock-detail")
include(":feature:portfolio")
include(":feature:sync")
