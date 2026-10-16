package com.gabrieltagama.menuplanner.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

/**
 * Pure Kotlin/JVM module without Android (the domain), compiled for the same Java version as the
 * Android modules.
 */
class JvmLibraryConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")
        pluginManager.apply("menuplanner.quality")
        extensions.configure<JavaPluginExtension> {
            toolchain.languageVersion.set(JavaLanguageVersion.of(BuildConfig.JAVA_VERSION.majorVersion))
        }
        tasks.withType<Test>().configureEach { failOnNoDiscoveredTests.set(false) }
        addUnitTestDependencies(android = false)
    }
}
