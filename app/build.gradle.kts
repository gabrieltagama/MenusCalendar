plugins {
    id("menuplanner.android.application")
    id("menuplanner.android.compose")
    id("menuplanner.hilt")
    alias(libs.plugins.kotlin.serialization)
}

val buildNumber: Int = providers.environmentVariable("GITHUB_RUN_NUMBER").map(String::toInt).getOrElse(1)
val releaseKeystorePath: String? = providers.environmentVariable("RELEASE_KEYSTORE_PATH").orNull

android {
    namespace = "com.gabrieltagama.menuplanner"

    defaultConfig {
        applicationId = "com.gabrieltagama.menuplanner"
        versionCode = buildNumber
        versionName = "1.0.$buildNumber"
    }

    signingConfigs {
        getByName("debug") {
            providers.environmentVariable("DEBUG_KEYSTORE_PATH").orNull?.let { path ->
                storeFile = file(path)
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
        if (releaseKeystorePath != null)
            create("release") {
                storeFile = file(releaseKeystorePath)
                storePassword = providers.environmentVariable("RELEASE_KEYSTORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("RELEASE_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("RELEASE_KEY_PASSWORD").get()
            }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":core:cloud"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:dishes"))
    implementation(project(":feature:calendar"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:share"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    implementation(libs.kotlinx.serialization.json)
}
