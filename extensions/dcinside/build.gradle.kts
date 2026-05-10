dependencies {
    compileOnly(project(":extensions:shared:library"))
    compileOnly(project(":extensions:dcinside:stub"))
    compileOnly(libs.annotation)
    compileOnly(libs.okhttp)
    compileOnly(libs.retrofit)
    compileOnly(libs.appcompat)
}

android {
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }
}
