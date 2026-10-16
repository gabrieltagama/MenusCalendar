package com.gabrieltagama.menuplanner.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Android application module: SDK levels, Java version, unit tests able to read Android resources
 * and the quality plugins. Identity, signing and versioning stay in the app build script.
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")
        pluginManager.apply("menuplanner.quality")
        extensions.configure<ApplicationExtension> {
            compileSdk = BuildConfig.COMPILE_SDK
            defaultConfig {
                minSdk = BuildConfig.MIN_SDK
                targetSdk = BuildConfig.TARGET_SDK
            }
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
