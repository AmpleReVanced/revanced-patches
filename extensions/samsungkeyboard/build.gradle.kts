dependencies {
    implementation(project(":extensions:shared:library"))
    compileOnly(project(":extensions:samsungkeyboard:stub"))
}

android {
    namespace = "app.revanced.extension.samsungkeyboard"
    compileSdk = 36

    defaultConfig {
        minSdk = 33
    }
}