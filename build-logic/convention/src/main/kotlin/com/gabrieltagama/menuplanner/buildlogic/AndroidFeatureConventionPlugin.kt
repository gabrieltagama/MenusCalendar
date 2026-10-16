package com.gabrieltagama.menuplanner.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/**
 * Feature module: an Android library with Compose, Hilt, type-safe navigation routes and the
 * dependencies every screen uses (domain, shared UI, lifecycle, navigation and Hilt ViewModels).
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("menuplanner.android.library")
        pluginManager.apply("menuplanner.android.compose")
        pluginManager.apply("menuplanner.hilt")
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
        dependencies {
            "implementation"(project(":core:domain"))
            "implementation"(project(":core:ui"))
            "implementation"(library("androidx-core-ktx"))
            "implementation"(library("androidx-lifecycle-runtime-compose"))
            "implementation"(library("androidx-lifecycle-viewmodel-compose"))
            "implementation"(library("androidx-navigation-compose"))
            "implementation"(library("androidx-hilt-lifecycle-viewmodel-compose"))
            "implementation"(library("kotlinx-serialization-json"))
        }
    }
}
