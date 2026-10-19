plugins {
    id("menuplanner.android.feature")
}

android {
    namespace = "com.gabrieltagama.menuplanner.feature.dishes"
}

dependencies {
    implementation(libs.androidx.activity.compose)
}
