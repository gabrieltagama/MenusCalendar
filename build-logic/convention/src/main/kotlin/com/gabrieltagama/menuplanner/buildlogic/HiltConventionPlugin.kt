package com.gabrieltagama.menuplanner.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Dependency injection with Hilt, its annotation processing running on KSP.
 */
class HiltConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.google.devtools.ksp")
        pluginManager.apply("com.google.dagger.hilt.android")
        dependencies {
            "implementation"(library("hilt-android"))
            "ksp"(library("hilt-compiler"))
        }
    }
}
