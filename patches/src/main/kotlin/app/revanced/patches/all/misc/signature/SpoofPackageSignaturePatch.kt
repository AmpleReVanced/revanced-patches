package app.revanced.patches.all.misc.signature

import app.morphe.patcher.PackageMetadata
import app.morphe.patcher.apk.ApkSignatureScheme
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import java.io.File
import java.security.cert.X509Certificate
import org.w3c.dom.Element

/**
 * The fully-qualified class name of the AppComponentFactory injected at patch time. The runtime
 * hook reads the original signing certificate packaged by [spoofPackageSignatureResourcePatch] and
 * patches every PackageManager response referring to the patched app.
 */
private const val APP_COMPONENT_FACTORY =
    "app.revanced.extension.all.misc.signature.spoof.SignatureSpoofAppComponentFactory"

/** Resource inside the patched APK holding the resolved original AppComponentFactory class name. */
private const val ORIGINAL_APP_COMPONENT_FACTORY_RESOURCE = "app.revanced.signature_spoof.app_component_factory"

/**
 * Meta-data entry declared on the {@code <application>} tag holding the original AppComponentFactory
 * class name. Used as a fallback when the resource file is missing or stripped.
 */
private const val ORIGINAL_APP_COMPONENT_FACTORY_META_DATA =
    "app.revanced.extension.all.misc.signature.spoof.ORIGINAL_APP_COMPONENT_FACTORY"

/** Resource inside the patched APK holding the hex-encoded original X.509 signing certificate. */
private const val SIGNATURE_RESOURCE = "app.revanced.signature_spoof.sig"
private const val SIGNATURE_RESOURCE_V31 = "app.revanced.signature_spoof.sig.v31"
private const val SIGNATURE_RESOURCE_V3 = "app.revanced.signature_spoof.sig.v3"
private const val SIGNATURE_RESOURCE_V2 = "app.revanced.signature_spoof.sig.v2"

/** Resource inside the patched APK holding the original package name. */
private const val PACKAGE_NAME_RESOURCE = "app.revanced.signature_spoof.package"

/**
 * Order in which schemes are inspected to pick the original signing certificate. Picking the
 * scheme that the current OS uses natively makes the {@link android.content.pm.Signature} returned
 * at runtime byte-identical to what the OS would return for a non-patched build.
 */
private val SIGNATURE_SCHEME_PREFERENCE = listOf(
    ApkSignatureScheme.V31,
    ApkSignatureScheme.V3,
    ApkSignatureScheme.V2,
)

/** AppComponentFactory requires API 28 (Android 9). */
private const val MIN_SDK_VERSION = 28

/**
 * Resource patch that prepares the patched APK for runtime signature spoofing:
 *
 *  - Writes the original signing certificate(s) (hex-encoded) into the APK using the new
 *    [PackageMetadata.signingCertificates] API exposed by morphe-patcher 1.6.
 *  - Writes the original package name.
 *  - Patches {@code AndroidManifest.xml} so that {@code android:appComponentFactory} points to
 *    [APP_COMPONENT_FACTORY], stores the previous factory in a resource file and as a
 *    {@code <meta-data>} entry, and bumps the minimum SDK to 28 if needed.
 */
private val spoofPackageSignatureResourcePatch = resourcePatch {
    execute {
        val signingCertificates = packageMetadata.signingCertificates
        val originalCertificate = originalCertificate(signingCertificates)
            ?: throw PatchException(
                "Could not find a signing certificate in the input APK. " +
                    "Use a signed APK with an APK Signing Block certificate."
            )

        val signatureHex = originalCertificate.encoded.toHex()

        val rootDir = get(".")

        // Primary signature resource used for any OS that does not match a scheme-specific file.
        File(rootDir, SIGNATURE_RESOURCE).writeText("$signatureHex\n")

        // Scheme-specific signature resources so the runtime can pick the one that matches what
        // the OS would expose natively for the running Android version.
        writeCertificateResource(signingCertificates, rootDir, ApkSignatureScheme.V31, SIGNATURE_RESOURCE_V31)
        writeCertificateResource(signingCertificates, rootDir, ApkSignatureScheme.V3, SIGNATURE_RESOURCE_V3)
        writeCertificateResource(signingCertificates, rootDir, ApkSignatureScheme.V2, SIGNATURE_RESOURCE_V2)

        File(rootDir, PACKAGE_NAME_RESOURCE).writeText("${packageMetadata.packageName}\n")

        document("AndroidManifest.xml").use { document ->
            val manifestElement = document.documentElement
                ?: throw PatchException("Could not find manifest root node.")

            val manifestPackage = manifestElement.getAttribute("package")
                .takeUnless { it.isBlank() }
                ?: packageMetadata.packageName

            requireMinSdkVersion(manifestElement)

            val applicationNode = document.getElementsByTagName("application").item(0)
                as? Element
                ?: throw PatchException("Could not find application node in manifest.")

            val originalComponentFactory =
                resolveManifestClassName(
                    applicationNode.getAttribute("android:appComponentFactory"),
                    manifestPackage
                )

            if (originalComponentFactory.isNotBlank()
                && originalComponentFactory != APP_COMPONENT_FACTORY
            ) {
                File(rootDir, ORIGINAL_APP_COMPONENT_FACTORY_RESOURCE)
                    .writeText("$originalComponentFactory\n")
                addOrUpdateMetaData(
                    applicationNode,
                    ORIGINAL_APP_COMPONENT_FACTORY_META_DATA,
                    originalComponentFactory
                )
            }

            applicationNode.setAttribute("android:appComponentFactory", APP_COMPONENT_FACTORY)
        }
    }
}

/**
 * Universal patch that hooks the Android PackageManager and WebViewUpdateService so the patched
 * app continues to see its original signing certificate and original package name at runtime,
 * bypassing integrity / Play Integrity / Play Protect signature checks performed inside the app.
 *
 * Requires a signed input APK (any APK Signing Block scheme: v2, v3 or v3.1) and Android 9+.
 */
@Suppress("unused")
val spoofPackageSignaturePatch = bytecodePatch(
    name = "Spoof package signature",
    description = "Hooks the Package Manager to return the original APK signing certificate " +
        "for the patched app. Requires Android 9 or later.",
    default = false,
) {
    extendWith("extensions/all/misc/signature/spoof-signature.mpe")
    dependsOn(spoofPackageSignatureResourcePatch)

    execute { /* NOP - the extension initializes itself via AppComponentFactory */ }
}

/**
 * Returns the X.509 signing certificate preferred for spoofing, or {@code null} if no signature
 * scheme is present in the input APK.
 */
private fun originalCertificate(
    signingCertificates: Map<ApkSignatureScheme, List<X509Certificate>>,
): X509Certificate? {
    for (scheme in SIGNATURE_SCHEME_PREFERENCE) {
        signingCertificates[scheme]?.firstOrNull()?.let { return it }
    }
    return signingCertificates.values.asSequence().flatten().firstOrNull()
}

/**
 * Writes the X.509 certificate for the given [scheme] (if any) to a resource file inside the
 * patched APK. The runtime extension selects the matching scheme for the running Android version.
 */
private fun writeCertificateResource(
    signingCertificates: Map<ApkSignatureScheme, List<X509Certificate>>,
    rootDir: File,
    scheme: ApkSignatureScheme,
    resourceName: String,
) {
    val certificate = signingCertificates[scheme]?.firstOrNull() ?: return
    File(rootDir, resourceName).writeText("${certificate.encoded.toHex()}\n")
}

private fun ByteArray.toHex(): String =
    joinToString(separator = "") { "%02x".format(it.toInt() and 0xFF) }

/**
 * Resolves a manifest class name to its fully-qualified form. The Android manifest allows the
 * {@code android:appComponentFactory} attribute to be relative to the manifest package name
 * (starting with a dot), or to be a single token without any dots.
 */
private fun resolveManifestClassName(className: String, packageName: String): String = when {
    className.isBlank() -> className
    className.startsWith(".") -> packageName + className
    className.contains(".") -> className
    else -> "$packageName.$className"
}

/**
 * Ensures the manifest declares {@code android:minSdkVersion >= 28}, otherwise the
 * {@code android:appComponentFactory} attribute is ignored by the platform.
 */
private fun requireMinSdkVersion(manifestElement: Element) {
    val usesSdkElement = (manifestElement.getElementsByTagName("uses-sdk").item(0) as? Element)
        ?: manifestElement.ownerDocument.createElement("uses-sdk").also {
            manifestElement.insertBefore(it, manifestElement.firstChild)
        }

    val currentMinSdk = usesSdkElement.getAttribute("android:minSdkVersion").toIntOrNull()
    if (currentMinSdk == null || currentMinSdk < MIN_SDK_VERSION) {
        usesSdkElement.setAttribute("android:minSdkVersion", "$MIN_SDK_VERSION")
    }
}

/**
 * Adds or updates a {@code <meta-data>} child of an {@code <application>} element. Used to declare
 * the original AppComponentFactory class name as a fallback to the resource file.
 */
private fun addOrUpdateMetaData(application: Element, name: String, value: String) {
    val existing = (0 until application.getElementsByTagName("meta-data").length)
        .asSequence()
        .map { application.getElementsByTagName("meta-data").item(it) }
        .filterIsInstance<Element>()
        .firstOrNull { it.getAttribute("android:name") == name }

    val metaData = existing ?: application.ownerDocument.createElement("meta-data").also {
        application.appendChild(it)
    }

    metaData.setAttribute("android:name", name)
    metaData.setAttribute("android:value", value)
}
