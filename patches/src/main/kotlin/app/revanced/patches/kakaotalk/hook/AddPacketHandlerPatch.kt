package app.revanced.patches.kakaotalk.hook

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.revanced.patches.kakaotalk.hook.fingerprints.LocoConnectionConstructorFingerprint
import app.revanced.patches.kakaotalk.hook.fingerprints.LocoMethodClassFingerprint
import app.revanced.patches.kakaotalk.hook.fingerprints.LocoProtocolResponseHookFingerprint
import app.revanced.patches.kakaotalk.hook.fingerprints.LocoReqBuilderConstructorFingerprint
import app.revanced.patches.kakaotalk.misc.addExtensionPatch
import app.revanced.patches.kakaotalk.misc.sharedExtensionPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.util.localRegisterCount
import com.android.tools.smali.dexlib2.Opcode
import org.w3c.dom.Element

private const val PACKET_HOOK_CLASS = "Lapp/revanced/extension/kakaotalk/packet/PacketHook;"
private const val REMOTE_PACKET_HANDLER_CLASS = "Lapp/revanced/extension/kakaotalk/packet/RemotePacketHandler;"

private val addPacketHandlerResourcesPatch = resourcePatch {
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

            val metaDataNodes = startupProvider.getElementsByTagName("meta-data")
            for (i in 0 until metaDataNodes.length) {
                val metaData = metaDataNodes.item(i) as Element
                if (metaData.getAttribute("android:name") == "app.revanced.extension.kakaotalk.packet.HookInitializer") {
                    return@use
                }
            }

            val metaData = document.createElement("meta-data")
            metaData.setAttribute("android:name", "app.revanced.extension.kakaotalk.packet.HookInitializer")
            metaData.setAttribute("android:value", "androidx.startup")

            startupProvider.appendChild(metaData)
        }
    }
}

@Suppress("unused")
val addPacketHandlerPatch = bytecodePatch(
    name = "Add Packet Handler",
    description = "Adding the Loco Packet Handler allows external applications to handle the app's packets, which may compromise security.\n" +
            "Conflicts may occur on some systems.",
    default = false
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(sharedExtensionPatch, addExtensionPatch, addPacketHandlerResourcesPatch)

    execute {
        LocoProtocolResponseHookFingerprint.method.addInstructions(
            0,
            """
                invoke-static {p1}, $PACKET_HOOK_CLASS->handlePacket(Ljava/lang/Object;)V
            """.trimIndent()
        )

        val locoMethodClass = LocoMethodClassFingerprint.classDef.type
        val locoReqBuilderConstructor = LocoReqBuilderConstructorFingerprint.method
        val locoReqBuilderClass = LocoReqBuilderConstructorFingerprint.classDef.type

        if (locoReqBuilderConstructor.parameterTypes[0].toString() != locoMethodClass) {
            throw PatchException("Could not verify LocoReq.Builder method parameter")
        }

        LocoConnectionConstructorFingerprint.method.apply {
            if (localRegisterCount < 2) {
                throw PatchException("LocoConnection constructor has too few local registers")
            }

            val returnIndex = implementation!!.instructions.indexOfLast {
                it.opcode == Opcode.RETURN_VOID
            }
            if (returnIndex < 0) {
                throw PatchException("Could not find LocoConnection constructor return")
            }

            addInstructions(
                returnIndex,
                """
                    const-string v0, "$locoMethodClass"
                    const-string v1, "$locoReqBuilderClass"
                    invoke-static {v0, v1, p0}, $REMOTE_PACKET_HANDLER_CLASS->initializeLoco(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;)V
                """.trimIndent()
            )
        }
    }
}
