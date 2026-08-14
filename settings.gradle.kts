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
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "FileForgePro"

include(":app")
include(":core:common")
include(":core:filesystem")
include(":core:storage")
include(":core:permissions")
include(":core:security")
include(":core:navigation")
include(":core:ui")
include(":data")
include(":domain")
include(":engine")
include(":feature:home")
include(":feature:browser")
include(":feature:search")
include(":feature:recent")
include(":feature:favorites")
include(":feature:properties")
include(":feature:storage")
include(":feature:analyzer")
include(":feature:archive")
include(":feature:media")
include(":feature:texteditor")
include(":feature:apk")
include(":feature:vault")
include(":feature:network")
include(":feature:settings")
include(":feature:windows")
