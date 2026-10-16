plugins {
    id("menuplanner.android.feature")
}

android {
    namespace = "com.gabrieltagama.menuplanner.feature.calendar"
}

dependencies {
    implementation(libs.androidx.activity.compose)
}
