android {
    namespace = "app.revanced.extension"

    defaultConfig {
        minSdk = 28 // AppComponentFactory only works on Android 9 (API 28) and above.
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    compileOnly(libs.annotation)
    implementation(libs.hiddenapibypass)
}