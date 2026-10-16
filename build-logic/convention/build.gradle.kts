plugins {
    `kotlin-dsl`
}

group = "com.gabrieltagama.menuplanner.buildlogic"

dependencies {
    compileOnly(libs.android.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "menuplanner.android.application"
            implementationClass = "com.gabrieltagama.menuplanner.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "menuplanner.android.library"
            implementationClass = "com.gabrieltagama.menuplanner.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "menuplanner.android.compose"
            implementationClass = "com.gabrieltagama.menuplanner.buildlogic.AndroidComposeConventionPlugin"
        }
        register("androidFeature") {
            id = "menuplanner.android.feature"
            implementationClass = "com.gabrieltagama.menuplanner.buildlogic.AndroidFeatureConventionPlugin"
        }
        register("hilt") {
            id = "menuplanner.hilt"
            implementationClass = "com.gabrieltagama.menuplanner.buildlogic.HiltConventionPlugin"
        }
        register("jvmLibrary") {
            id = "menuplanner.jvm.library"
            implementationClass = "com.gabrieltagama.menuplanner.buildlogic.JvmLibraryConventionPlugin"
        }
        register("quality") {
            id = "menuplanner.quality"
            implementationClass = "com.gabrieltagama.menuplanner.buildlogic.QualityConventionPlugin"
        }
    }
}
