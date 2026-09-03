plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "app.revanced.extension.kakaomap.stub"
    compileSdk = 36

    defaultConfig {
        minSdk = 29
    }
}