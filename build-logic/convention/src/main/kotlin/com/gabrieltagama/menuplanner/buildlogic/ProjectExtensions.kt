package com.gabrieltagama.menuplanner.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

/**
 * Shared helpers of the convention plugins (the SDK levels live in BuildConfig): typed access to
 * the version catalog and the unit test dependencies (JUnit, coroutines,
 * Turbine and, for Android modules, Robolectric so Room, ViewModel and Compose tests run on the JVM).
 */
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
