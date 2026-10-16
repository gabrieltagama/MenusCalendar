plugins {
    id("menuplanner.android.library")
    id("menuplanner.android.compose")
}

android {
    namespace = "com.gabrieltagama.menuplanner.core.ui"
}

dependencies {
    api(project(":core:domain"))

    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
    api(libs.androidx.compose.ui.tooling.preview)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.material.icons.extended)
}
