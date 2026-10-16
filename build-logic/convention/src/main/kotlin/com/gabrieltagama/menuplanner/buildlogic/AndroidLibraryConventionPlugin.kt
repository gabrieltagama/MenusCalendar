package com.gabrieltagama.menuplanner.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Android library module: SDK levels, Java version, unit tests able to read Android resources
 * (Robolectric) and the quality plugins.
 */
class AndroidLibraryConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")
        pluginManager.apply("menuplanner.quality")
        extensions.configure<LibraryExtension> {
            compileSdk = BuildConfig.COMPILE_SDK
            defaultConfig.minSdk = BuildConfig.MIN_SDK
            compileOptions {
                sourceCompatibility = BuildConfig.JAVA_VERSION
                targetCompatibility = BuildConfig.JAVA_VERSION
            }
            testOptions.unitTests {
                isIncludeAndroidResources = true
                all { it.jvmArgs(BuildConfig.ROBOLECTRIC_JVM_ARGS) }
            }
        }
        addUnitTestDependencies(android = true)
    }
}
