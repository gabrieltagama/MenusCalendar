package com.gabrieltagama.menuplanner.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Code quality of every module: Kover measures unit test coverage (merged in the root project) and
 * ktlint checks the Kotlin style defined in .editorconfig.
 */
class QualityConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlinx.kover")
        pluginManager.apply("org.jlleitschuh.gradle.ktlint")
    }
}
