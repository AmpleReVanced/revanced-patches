# Riro School 7.5.6

Supported target: `com.rirosoft.riroschool`, version `7.5.6`, versionCode `179`.
Original APK SHA-256: `5a9292143f1ace579e2f37f9bb81867a58345b8a9e4b68d9bed6575b39b03ed4`.

| Patch | Behavior | Default |
|---|---|---|
| Disable Firebase analytics | Suppresses Analytics/Measurement, Crashlytics, Sessions and app FIS/FCM entry points. Native `fcmCode` is omitted. **Push is unavailable.** | On |
| Disable Google analytics | Suppresses ML Kit diagnostic logging and installation-ID preload/fallback; removes advertising permissions. Does not spoof GMS availability. | On |
| Disable Rirosoft analytics | Omits native `device_id` key/value, including repeated or encoded root form keys. Retains the local UUID. | On |
| Disable VPN detection | Removes app-local VPN queries, warning and logs. Retains real connectivity validation. | On |
| Disable ADB detection | Removes app-local ADB and attached-debugger detection. Retains other lifecycle behavior. | On |
| Spoof GMS availability | Treats only the app startup GMS availability result as SUCCESS. Does not install GMS or change analytics. | **Off** |

Firebase and Google share an internal dependency that discards all app DataTransport/CCT telemetry. GMS spoofing is independent. No global networking, Android settings or framework availability API is modified.

BLE activities/services, callbacks, permissions, bridge methods, attendance requests and their distinct `deviceid` field remain intact. These patches do not fabricate successful attendance. Authenticated login, student-card behavior, physical BLE attendance and real barcode-module installation are not guaranteed. Missing native identifiers or push registration may affect the server contract.

Previously queued SDK data may remain on disk, with its selected transmission paths blocked. Remote WebView content, server logs, account cookies, IP addresses, BLE and actual GMS modules can still expose information. This is not a claim of zero tracking or zero Google traffic.

## Integration

The implementation follows this repository's Patcher 2.0.0 bytecode/resource APIs and its app-specific package structure. The runtime form helper belongs to `extensions/riroschool`, is injected only through the Rirosoft patch, and is not a patch-host dependency. The repository's existing version and Semantic Release process own bundle versioning; there is no separate Riro School bundle version.

## Original-artifact regression checks

No application APK or signing key is included. Supply an original APK and its decoded manifest locally:

```sh
apkanalyzer manifest print original.apk > original-manifest.xml
./gradlew :patches:verifyRiroSchool \
  -PriroschoolApk=/absolute/path/original.apk \
  -PriroschoolManifest=/absolute/path/original-manifest.xml
```

The opt-in checks validate exact target rejection, detector removal and retained control flow, Firebase caller closure, ML Kit ID paths, native form filtering, local UUID preservation and unchanged BLE manifest declarations. They include duplicate/encoded keys and a large form input. This task does not execute or install the supplied APK.

## Integration validation (2026-11-11)

- **Full repository Gradle verification now passes:** `:patches:buildAndroid`, `generatePatchesList` (including `:patches:build`) and `:patches:verifyRiroSchool`. The initial HTTP **401** was resolved locally by importing official, checksum-verified public Maven artifacts and using a temporary init script. Upstream repository URLs, API versions and release settings remain unchanged. See the [Gradle verification report](riroschool-gradle-verification.md) for commands, hashes and limitations.
- Standalone artifact checks now live in `src/riroSchoolVerification`, separate from JUnit discovery. The Java-only Riro extension excludes the implicit Kotlin runtime; its DEX contains only the form helper and the standard empty generated `R` class.
- The actual Gradle bundle loads **134** public patches. Applying all six Riro patches passed **153** artifact checks and **34** emulator runtime checks. All 7,193 original classes remain: 22 approved changed classes, 7,171 unchanged classes, and two added extension classes. The final bundle's executable payload is byte-identical to the tested bundle; its manifest timestamp differs between builds.
- Earlier compatibility checks used a temporary composite of the published 1.5.0 bundle and newly compiled classes. Its **154** checks and 52-failure unpatched negative control are historical evidence; it is not the actual Gradle output. The current Gradle artifact checks explicitly account for the generated empty `R` class and do not assume the composite's non-DEX entries are byte-identical.
- Independent CLI selection checks passed: Google-only changed 13 classes and retained the original startup GMS query; GMS-only changed just `IntroActivity` and preserved the original binary manifest. GMS remains opt-in and is not a Google analytics dependency.
- Android **37**, x86_64, isolated emulator: ART verification and **34** runtime checks passed with ADB enabled and GMS disabled. Startup, ML Kit diagnostics cleanup, suppressed native identifiers and no-token behavior were exercised; GMS and connectivity settings were restored afterward. These checks do not cover authenticated server workflows or physical BLE attendance.

Keep the repository's existing `v1.5.0` baseline and Conventional Commit/Semantic Release flow. Generated version fields, bundle metadata and the generated patch-list section are updated by that flow, rather than copied from a standalone build.
