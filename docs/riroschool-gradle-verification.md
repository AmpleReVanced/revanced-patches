# Riro School Gradle verification

Verified on **2026-11-11 (Asia/Seoul)**. Full repository Gradle build and opt-in original-artifact regression checks **passed**. No release was published.

## Environment and dependency resolution

- Windows, JDK 17.0.9, repository wrapper **Gradle 9.7.1**.
- Unchanged Morphe Gradle plugin, Patcher and patch/extension libraries: **2.0.0**. Kotlin **2.4.21**, AGP **9.1.0**; Android platforms **34 / 36**, Build Tools **36.0.0**.
- Original target: `com.rirosoft.riroschool` **7.5.6 / 179**, SHA-256 `5a9292143f1ace579e2f37f9bb81867a58345b8a9e4b68d9bed6575b39b03ed4`.
- The configured GitHub Packages Maven endpoint initially returned HTTP 401. The public package pages expose downloadable artifacts even though Maven endpoint authentication failed. Official POM, module metadata and binaries were imported into a temporary local Maven repository after verifying their published SHA-256 checksums.
- A local Gradle init script added that repository for `app.morphe` groups. Neither upstream repository configuration nor credentials were changed. The configured online Maven endpoint's authentication remains unresolved; the successful build uses the documented local artifact import.

Official public package sources, all at version **2.0.0**:

| Package | Source | Verified binary/POM SHA-256 |
|---|---|---|
| Plugin marker POM | [2766979](https://github.com/MorpheApp/morphe-patches-gradle-plugin/packages/2766979?version=2.0.0) | `9e385c540bcf4aa930e9777f7d53c05695b455e77b958cc99ec2cce29593876c` |
| Gradle plugin JAR | [2766983](https://github.com/MorpheApp/morphe-patches-gradle-plugin/packages/2766983?version=2.0.0) | `65020448ff5deef1195416738bb50f833ed59bdc2f6752900ef2b2eb8f31d22d` |
| Patcher JAR | [2772021](https://github.com/MorpheApp/morphe-patcher/packages/2772021?version=2.0.0) | `c7517e315251b6cc48ac01610bcaa15a84c019b6738eb9e4d34ee957880a921a` |
| Patch library JAR | [2947831](https://github.com/MorpheApp/morphe-patches-library/packages/2947831?version=2.0.0) | `c95e9cf74e6a11327317b12e43e504c1ad318a3b54526101f8d4d35e0ddc5585` |
| Extension library AAR | [2947832](https://github.com/MorpheApp/morphe-patches-library/packages/2947832?version=2.0.0) | `bf8ca14f59de9fb29a3b840796bc19685712619489ec0ea4b662b7ccd3f8db66` |

Retain the Maven directory layout, POMs and module metadata alongside binaries. Public non-Morphe dependencies were resolved normally. No dependency was replaced with classes extracted from Morphe Desktop.

The temporary init script used this configuration (replace only the local directory):

```groovy
def localMorpheRepo = new File('/absolute/path/morphe-maven')
gradle.beforeSettings { settings ->
    settings.pluginManagement.repositories.maven {
        url = localMorpheRepo.toURI()
        content { includeGroupByRegex('app\\.morphe(\\..*)?') }
    }
    settings.dependencyResolutionManagement.repositories.maven {
        url = localMorpheRepo.toURI()
        content { includeGroupByRegex('app\\.morphe(\\..*)?') }
    }
}
```

## Build and regression results

The CI task graph and opt-in checks were run together:

```sh
./gradlew :patches:buildAndroid generatePatchesList :patches:verifyRiroSchool \
  --init-script /absolute/path/morphe-local-maven.init.gradle \
  -PriroschoolApk=/absolute/path/original.apk \
  -PriroschoolManifest=/absolute/path/original-manifest.xml
```

The same graph also passed after cleaning the Riro extension and patches build directories. The final run, with UTF-8 explicitly set for verification Java compilation, reported **BUILD SUCCESSFUL**, **364 actionable tasks: 7 executed, 357 up-to-date**, and the regression runner's `PASS: target, detector, identity, Firebase and ML Kit contracts`.

- `:patches:buildAndroid`: passed; generated `patches-1.5.0.mpp`.
- `generatePatchesList`, including `:patches:build`: passed; generated **134** public entries, including six Riro patches.
- `:patches:verifyRiroSchool`: passed against the original APK and manifest.
- Default `:patches:test`: `NO-SOURCE`. This is not a JUnit test pass count; the artifact-dependent checks execute through their dedicated JavaExec task.
- Nonfatal Gradle deprecation warnings remain. Gradle 10 compatibility is not certified.

Build fixes discovered by this validation:

1. Moved standalone Kotlin runners into `src/riroSchoolVerification`, associated with the main compilation for internal API access. This avoids Gradle's no-tests-discovered failure without disabling JUnit tests.
2. Set the verification helper's Java compilation to release 11 and UTF-8 for Windows.
3. Excluded the implicit Kotlin standard library only from the Java-only Riro extension's runtime classpaths, removing **1,274** unnecessary classes from the patched APK. Its extension DEX is now **2,816 bytes**, containing `FormParameters` and the standard generated empty namespace `R` class. Other extensions retain their dependency configuration.

## Actual bundle and APK checks

- The real Gradle bundle loaded **134** public patches in Patcher **2.0.0** through official Morphe Desktop **1.19.0**.
- Installed Morphe Desktop **2.1.1** applied all six patches successfully.
- Independent output verification passed **153** checks. The APK has **7,195** classes across four DEX files: all **7,193** original classes retained, **22** approved changed classes, **7,171** unchanged classes, and the two extension classes above. The generated `R` class has no fields or functional behavior; the helper matches the packaged extension. Approved manifest transformations and BLE preservation passed.
- On the isolated Android **37** x86_64 emulator, ART verification and **34** runtime checks passed with ADB enabled and GMS disabled. GMS and connectivity were restored afterward. Screenshot capture encountered an emulator **System UI ANR**; this run certifies the instrumented checks and startup command, not complete interactive UI navigation. Authenticated workflows and physical BLE attendance remain unverified.
- The final Gradle bundle's non-manifest ZIP entries were compared byte-for-byte with the statically and runtime-tested bundle and matched. Rebuilding changes the manifest timestamp, so whole-bundle hashes differ.

| Artifact | SHA-256 |
|---|---|
| Bundle used for APK/static/runtime checks | `0652db37cde0f6ac4ed7e0c98ebe75035baf0816b688a2b1d022b348674a9507` |
| Final Gradle bundle after UTF-8 verification fix | `c8de1407be0f1ecdfdd30a1a565f47a6de5b0c34bae9137a81ddcc8f970c451c` |

These are local validation artifacts, not a new release or version bump. The upstream AmpleReVanced endpoints, existing `1.5.0` version baseline and Semantic Release configuration remain unchanged.
