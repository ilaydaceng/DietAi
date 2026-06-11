rootProject.name = "DietAiApp"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google() // Standart Google deposu
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        google() // Gemini buradadır
        mavenCentral()
    }
}

include(":composeApp")