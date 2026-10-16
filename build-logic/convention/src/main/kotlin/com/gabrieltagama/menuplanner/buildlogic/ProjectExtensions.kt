package com.gabrieltagama.menuplanner.buildlogic

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

/**
 * Shared values and helpers of the convention plugins: SDK levels and Java version used by every
 * module, typed access to the version catalog and the unit test dependencies (JUnit, coroutines,
 * Turbine and, for Android modules, Robolectric so Room, ViewModel and Compose tests run on the JVM).
 */
internal object BuildConfig {
    const val COMPILE_SDK = 37
    const val TARGET_SDK = 36
    const val MIN_SDK = 31
    val JAVA_VERSION: JavaVersion = JavaVersion.VERSION_17
    val ROBOLECTRIC_JVM_ARGS = listOf(
        "--add-opens=java.base/java.io=ALL-UNNAMED",
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
        "--add-opens=java.base/java.nio=ALL-UNNAMED",
        "--add-opens=java.base/java.util=ALL-UNNAMED",
        "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
        "--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED",
        "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED"
    )
}

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun Project.library(alias: String): Provider<MinimalExternalModuleDependency> = libs.findLibrary(alias).get()

internal fun Project.addUnitTestDependencies(android: Boolean) = dependencies {
    "testImplementation"(library("junit"))
    "testImplementation"(library("kotlinx-coroutines-test"))
    "testImplementation"(library("turbine"))
    if (android) {
        "testImplementation"(library("robolectric"))
        "testImplementation"(library("androidx-test-core"))
        "testImplementation"(library("androidx-test-ext-junit"))
    }
}
