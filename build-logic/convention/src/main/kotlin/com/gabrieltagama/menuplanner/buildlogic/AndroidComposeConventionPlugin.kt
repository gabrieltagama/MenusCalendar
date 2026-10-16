package com.gabrieltagama.menuplanner.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Enables Jetpack Compose in an Android module (library or application) with the Compose BOM,
 * the tooling for previews and the Compose test rule for Robolectric unit tests.
 */
class AndroidComposeConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        pluginManager.withPlugin("com.android.library") {
            extensions.configure<LibraryExtension> { buildFeatures.compose = true }
        }
        pluginManager.withPlugin("com.android.application") {
            extensions.configure<ApplicationExtension> { buildFeatures.compose = true }
        }
        dependencies {
            val bom = platform(library("androidx-compose-bom"))
            "implementation"(bom)
            "testImplementation"(bom)
            "debugImplementation"(library("androidx-compose-ui-tooling"))
            "testImplementation"(library("androidx-compose-ui-test-junit4"))
            "testImplementation"(library("androidx-test-espresso-core"))
            "debugImplementation"(library("androidx-compose-ui-test-manifest"))
        }
    }
}
