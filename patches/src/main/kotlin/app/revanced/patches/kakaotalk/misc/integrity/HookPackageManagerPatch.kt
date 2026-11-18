package app.revanced.patches.kakaotalk.misc.integrity

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.document
import app.morphe.patcher.patch.patch
import app.morphe.patcher.patch.resourceFile
import app.revanced.patches.kakaotalk.misc.extension.addExtensionPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import java.io.File

@Suppress("unused")
val hookPackageManagerPatch = patch(
    name = "Hook Package Manager",
    description = "Hooks the Package Manager to bypass integrity checks.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(addExtensionPatch)

    execute {
        document("AndroidManifest.xml").use { document ->
            val applicationNode = document
                .getElementsByTagName("application")
                .item(0) as org.w3c.dom.Element

            applicationNode.setAttribute(
                "android:appComponentFactory",
                "app.revanced.extension.kakaotalk.spoofer.RevancedAppComponentFactory"
            )
        }

        val sig =
            ::javaClass.javaClass.classLoader.getResourceAsStream("kakaotalk/app.revanced.sig.orig")?.readAllBytes()
                ?: throw PatchException("Failed to load original signature resource.")

        val rootDir = resourceFile(".")
        val sigFile = File(rootDir, "app.revanced.sig.orig")
        sigFile.writeBytes(sig)
    }
}