plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.kover)
}

dependencies {
    kover(project(":app"))
    kover(project(":core:domain"))
    kover(project(":core:data"))
    kover(project(":core:ui"))
    kover(project(":core:cloud"))
    kover(project(":core:cloud-ui"))
    kover(project(":feature:onboarding"))
    kover(project(":feature:dishes"))
    kover(project(":feature:calendar"))
    kover(project(":feature:settings"))
    kover(project(":feature:share"))
}

kover {
    reports {
        verify {
            rule("Cobertura mínima de líneas") {
                minBound(59)
            }
        }
    }
}
