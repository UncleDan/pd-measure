// settings.gradle.kts by Daniele Lolli (UncleDan) feat. Claude AI - Release 1.1.0 - 2026-10-24 21-02-56
pluginManagement {
    repositories {
        google()
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
rootProject.name = "pd-measure"
include(":app")
