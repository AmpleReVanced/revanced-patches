package app.revanced.patches.kakaotalk.hook

import app.morphe.patcher.patch.resourcePatch
import app.revanced.patches.kakaotalk.misc.addExtensionPatch
import app.revanced.patches.kakaotalk.misc.addPineLibPatch
import app.revanced.patches.kakaotalk.misc.sharedExtensionPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import org.w3c.dom.Element

@Suppress("unused")
val addPacketHandlerPatch = resourcePatch(
    name = "Add Packet Handler",
    description = "Adding the Loco Packet Handler allows external applications to handle the app's packets, which may compromise security.\n" +
            "Conflicts may occur on some systems.",
    default = false
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(sharedExtensionPatch, addExtensionPatch, addPineLibPatch)

    execute {
        document("AndroidManifest.xml").use { document ->
            val manifest = document.getElementsByTagName("manifest").item(0) as Element
            val application = document.getElementsByTagName("application").item(0) as Element
            val packageName = manifest.getAttribute("package")

            val providers = application.getElementsByTagName("provider")
            var startupProvider: Element? = null

            for (i in 0 until providers.length) {
                val provider = providers.item(i) as Element
                if (provider.getAttribute("android:name") == "androidx.startup.InitializationProvider") {
                    startupProvider = provider
                    break
                }
            }

            if (startupProvider == null) {
                startupProvider = document.createElement("provider")
                startupProvider.setAttribute("android:name", "androidx.startup.InitializationProvider")
                startupProvider.setAttribute("android:exported", "false")
                startupProvider.setAttribute("android:authorities", "$packageName.androidx-startup")
                application.appendChild(startupProvider)
            }

            val metaData = document.createElement("meta-data")
            metaData.setAttribute("android:name", "app.revanced.extension.kakaotalk.packet.HookInitializer")
            metaData.setAttribute("android:value", "androidx.startup")

            startupProvider.appendChild(metaData)
        }
    }
}