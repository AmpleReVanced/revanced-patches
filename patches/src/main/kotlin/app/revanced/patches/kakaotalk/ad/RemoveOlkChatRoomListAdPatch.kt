package app.revanced.patches.kakaotalk.ad

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.returnEarly
import app.revanced.patches.kakaotalk.ad.fingerprints.AddOlkChatRoomListAdFingerprint
import app.revanced.patches.kakaotalk.ad.fingerprints.OpenChatTabFragmentAdEnabledFingerprint
import app.revanced.patches.kakaotalk.shared.fingerprints.KotlinUnitInstanceFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.util.smaliReference

@Suppress("unused")
val removeOlkChatRoomListAdPatch = bytecodePatch(
    name = "Remove OpenLink chat room list ad",
    description = "Removes the OpenLink chat room list ad.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        val unitClass = KotlinUnitInstanceFingerprint.classDef
        val unitInstance = unitClass.staticFields.singleOrNull { it.type == unitClass.type }
            ?: throw PatchException("Could not find Kotlin Unit instance")
        val method = AddOlkChatRoomListAdFingerprint.method
        val register = method.getFreeRegisterProvider(0, 1).getFreeRegister4Bit()

        method.addInstructions(
            0,
            """
                sget-object v$register, ${unitInstance.smaliReference}
                return-object v$register
            """.trimIndent()
        )

        OpenChatTabFragmentAdEnabledFingerprint.method.returnEarly(false)
    }
}
