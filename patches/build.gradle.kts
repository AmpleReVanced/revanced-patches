group = "app.ample"

patches {
    about {
        name = "Ample Patches"
        description = "Patches for Morphe"
        source = "git@github.com:mchany/revanced-patches"
        author = "Ample"
        contact = "na"
        website = "na"
        license = "GNU General Public License v3.0"
    }
}

dependencies {
    // Used by JsonGenerator.
    implementation(libs.gson)
    implementation(kotlin("metadata-jvm"))

    // Required due to smali, or build fails. Can be removed once smali is bumped.
    implementation(libs.guava)

    implementation(libs.morphe.patches.library)

    implementation(libs.apksig)

    // ELF symbol lookup for native patches.
    implementation(libs.jelf)

    // Android API stubs defined here.
    compileOnly(project(":patches:stub"))
}

sourceSets.named("test") {
    // Pure Java helper: compile it for JVM tests without packaging it into the patch JAR.
    java.srcDir(rootProject.file("extensions/riroschool/src/main/java"))
}

tasks {
    named<JavaCompile>("compileTestJava") {
        options.release.set(11)
    }

    register<JavaExec>("verifyRiroSchool") {
        description = "Verify Riro School patches against a user-supplied original APK and decoded manifest"
        group = "verification"
        dependsOn(testClasses)
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("app.revanced.patches.riroschool.PrivacyPatchTestsKt")
        doFirst {
            val apk = providers.gradleProperty("riroschoolApk").orNull ?: System.getenv("RIRO_TEST_APK")
            val manifest = providers.gradleProperty("riroschoolManifest").orNull ?: System.getenv("RIRO_TEST_MANIFEST")
            require(!apk.isNullOrBlank() && file(apk).isFile) { "Provide -PriroschoolApk=/path/to/original.apk" }
            require(!manifest.isNullOrBlank() && file(manifest).isFile) { "Provide -PriroschoolManifest=/path/to/original-manifest.xml" }
            environment("RIRO_TEST_APK", file(apk).absolutePath)
            environment("RIRO_TEST_MANIFEST", file(manifest).absolutePath)
        }
    }

    register<JavaExec>("checkStringResources") {
        description = "Checks resource strings for invalid formatting"

        dependsOn(build)

        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set("app.morphe.patches.util.resource.CheckStringResourcesKt")
    }

    register<JavaExec>("generatePatchesList") {
        description = "Build patch with patch list"

        dependsOn(build)

        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set("app.morphe.util.PatchListGeneratorKt")
    }
    // Used by gradle-semantic-release-plugin.
    publish {
        dependsOn("generatePatchesList")
    }
}

publishing {
    repositories {
        maven {
            name = "githubPackages"
            url = uri("https://maven.pkg.github.com/mchany/revanced-patches")
            credentials(PasswordCredentials::class)
        }
    }
}

//apply(from = "strings-processing.gradle.kts")
