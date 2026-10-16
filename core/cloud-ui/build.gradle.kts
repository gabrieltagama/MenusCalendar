plugins {
    id("menuplanner.android.library")
    id("menuplanner.android.compose")
    id("menuplanner.hilt")
}

android {
    namespace = "com.gabrieltagama.menuplanner.core.cloud.ui"
}

dependencies {
    implementation(project(":core:cloud"))

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.activity.compose)
}
