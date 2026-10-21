package com.gabrieltagama.menuplanner.buildlogic

import org.gradle.api.JavaVersion

/**
 * SDK levels, Java version and Robolectric JVM arguments shared by every module of the build.
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
