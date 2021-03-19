@file:Suppress("UnstableApiUsage")

pluginManagement {
    val kotlinVersion: String by settings

    // Preload versions of all plugins for all projects.
    // They should still be applied in the subproject's build.gradle.
    // Within subprojects, version should be omitted.
    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion

        val spotlessPluginVersion: String by settings
        id("com.diffplug.gradle.spotless") version spotlessPluginVersion apply false

        val detektPluginVersion: String by settings
        id("io.gitlab.arturbosch.detekt") version detektPluginVersion apply false

        val owaspDependencyCheckPluginVersion: String by settings
        id("org.owasp.dependencycheck") version owaspDependencyCheckPluginVersion apply false
    }

    repositories { // Only for these plugins. Repos for other dependencies should be defined in build.gradle.kts
        mavenLocal()
        google()
        mavenCentral()
        maven("https://plugins.gradle.org/m2/")
        maven("https://software.r3.com/artifactory/corda")
    }
}
