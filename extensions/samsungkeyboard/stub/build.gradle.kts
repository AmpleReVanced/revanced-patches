plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "app.revanced.extension"
    compileSdk = 36

    defaultConfig {
        minSdk = 33
    }
}