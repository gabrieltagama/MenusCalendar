plugins {
    id("menuplanner.android.feature")
}

android {
    namespace = "com.gabrieltagama.menuplanner.feature.onboarding"
}

dependencies {
    implementation(project(":core:cloud"))
}
