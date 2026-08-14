package app.revanced.patches.kakaotalk.misc.hook

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.getFreeRegisterProvider
import app.revanced.patches.kakaotalk.misc.hook.fingerprints.KakaoApplicationOnCreateFingerprint
import app.revanced.patches.kakaotalk.misc.hook.fingerprints.LocoConnectionConstructorFingerprint
import app.revanced.patches.kakaotalk.misc.hook.fingerprints.LocoMethodClassFingerprint
import app.revanced.patches.kakaotalk.misc.hook.fingerprints.LocoProtocolResponseHookFingerprint
import app.revanced.patches.kakaotalk.misc.hook.fingerprints.LocoReqBuilderConstructorFingerprint
import app.revanced.patches.kakaotalk.misc.extension.addExtensionPatch
import app.revanced.patches.kakaotalk.misc.extension.sharedExtensionPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.util.parameterRegister
import com.android.tools.smali.dexlib2.Opcode

private const val PACKET_HOOK_CLASS = "Lapp/revanced/extension/kakaotalk/packet/PacketHook;"
private const val REMOTE_PACKET_HANDLER_CLASS = "Lapp/revanced/extension/kakaotalk/packet/RemotePacketHandler;"

@Suppress("unused")
val addPacketHandlerPatch = bytecodePatch(
    name = "Add Packet Handler",
    description = "Adding the Loco Packet Handler allows external applications to handle the app's packets, which may compromise security.\n" +
            "Conflicts may occur on some systems.",
    default = false
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(sharedExtensionPatch, addExtensionPatch)

    execute {
        KakaoApplicationOnCreateFingerprint.method.apply {
            val returnIndex = implementation!!.instructions.indexOfLast {
                it.opcode == Opcode.RETURN_VOID
            }
            if (returnIndex < 0) {
                throw PatchException("Could not find Kakao application onCreate return")
            }

            addInstructions(
                returnIndex,
                """
                    invoke-static {p0}, Lapp/revanced/extension/kakaotalk/packet/HookInitializer;->initialize(Landroid/content/Context;)V
                """.trimIndent()
            )
        }

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
            val returnIndex = implementation!!.instructions.indexOfLast {
                it.opcode == Opcode.RETURN_VOID
            }
            if (returnIndex < 0) {
                throw PatchException("Could not find LocoConnection constructor return")
            }
            val receiverRegister = parameterRegister(0) - 1
            val registers = getFreeRegisterProvider(returnIndex, 2, receiverRegister)
            val methodTypeRegister = registers.getFreeRegister4Bit()
            val builderTypeRegister = registers.getFreeRegister4Bit()

            addInstructions(
                returnIndex,
                """
                    const-string v$methodTypeRegister, "$locoMethodClass"
                    const-string v$builderTypeRegister, "$locoReqBuilderClass"
                    invoke-static {v$methodTypeRegister, v$builderTypeRegister, p0}, $REMOTE_PACKET_HANDLER_CLASS->initializeLoco(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;)V
                """.trimIndent()
            )
        }
    }
}
