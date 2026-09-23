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

rootProject.name = "Estante"

// Parcial: Android Views (XML) + navegação por Intent explícita
include(":views")
// Etapa 2: Jetpack Compose
include(":app")
