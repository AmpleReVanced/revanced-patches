plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "app.revanced.extension"
    compileSdk = 36 // for hidden api access

    defaultConfig {
        minSdk = 24
    }
}
