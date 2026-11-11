android {
    namespace = "app.revanced.extension.riroschool"
    compileSdk = 36

    defaultConfig {
        minSdk = 21
    }
}

// This extension is pure Java; do not inject AGP's implicit Kotlin runtime into the target APK.
configurations.matching { it.name.endsWith("RuntimeClasspath") }.configureEach {
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
}
