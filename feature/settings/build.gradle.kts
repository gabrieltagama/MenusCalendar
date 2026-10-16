plugins {
    id("menuplanner.android.feature")
}

android {
    namespace = "com.gabrieltagama.menuplanner.feature.settings"
}

dependencies {
    implementation(project(":core:cloud"))
}
