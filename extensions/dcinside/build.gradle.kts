dependencies {
    compileOnly(project(":extensions:shared:library"))
    compileOnly(project(":extensions:dcinside:stub"))
    compileOnly(libs.annotation)
    compileOnly(libs.okhttp)
    compileOnly(libs.retrofit)
    compileOnly(libs.appcompat)
}

android {
    defaultConfig {
        minSdk = 26
    }
}
