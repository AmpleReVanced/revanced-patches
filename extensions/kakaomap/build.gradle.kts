dependencies {
    compileOnly(project(":extensions:kakaomap:stub"))
    compileOnly(project(":extensions:shared:library"))
    compileOnly(libs.annotation)
}

android {
    compileSdk = 36

    defaultConfig {
        minSdk = 29
    }
}