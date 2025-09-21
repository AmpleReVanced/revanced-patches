rootProject.name = "revanced-patches"

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/revanced/registry")
            credentials {
                username = providers.gradleProperty("gpr.user").getOrElse(System.getenv("GITHUB_ACTOR"))
                password = providers.gradleProperty("gpr.key").getOrElse(System.getenv("GITHUB_TOKEN"))
            }
        }
        maven {
            name = "GiteaPackages"
            url = uri("https://git.naijun.dev/api/packages/ReVanced/maven")
            credentials(HttpHeaderCredentials::class) {
                name = "Authorization"
                value = "token ${providers.gradleProperty("gitea.accessToken")}"
            }

            authentication { create<HttpHeaderAuthentication>("header") }
        }
    }
}

plugins {
    id("app.revanced.patches") version "1.0.0-dev.7"
}

settings {
    extensions {
        defaultNamespace = "app.revanced.extension"

        // Must resolve to an absolute path (not relative),
        // otherwise the extensions in subfolders will fail to find the proguard config.
        proguardFiles(rootProject.projectDir.resolve("extensions/proguard-rules.pro").toString())
    }
}

include(":patches:stub")
