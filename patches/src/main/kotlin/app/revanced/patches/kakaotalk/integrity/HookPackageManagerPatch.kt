package app.revanced.patches.kakaotalk.integrity

import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.kakaotalk.misc.addExtensionPatch
import java.io.File

@Suppress("unused")
val hookPackageManagerPatch = resourcePatch(
    name = "Hook Package Manager",
    description = "Hooks the Package Manager to bypass integrity checks.",
) {
    compatibleWith("com.kakao.talk"("26.1.0"))
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

        val rootDir = get(".")
        val sigFile = File(rootDir, "app.revanced.sig.orig")
        sigFile.writeBytes(sig)
    }
}