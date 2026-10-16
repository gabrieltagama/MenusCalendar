plugins {
    id("menuplanner.android.library")
    id("menuplanner.android.compose")
    id("menuplanner.hilt")
}

android {
    namespace = "com.gabrieltagama.menuplanner.core.cloud"
}

dependencies {
    implementation(project(":core:domain"))

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.play.services.auth)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
}
