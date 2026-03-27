package app.revanced.patches.kakaotalk.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.returnEarly
import app.revanced.patches.kakaotalk.ads.fingerprints.AddOlkChatRoomListAdFingerprint
import app.revanced.patches.kakaotalk.ads.fingerprints.OpenChatTabFragmentAdEnabledFingerprint
import app.revanced.patches.kakaotalk.common.fingerprints.KotlinUnitInstanceFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO

@Suppress("unused")
val removeOlkChatRoomListAdPatch = bytecodePatch(
    name = "Remove OpenLink chat room list ad",
    description = "Removes the OpenLink chat room list ad.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        val unitClass = KotlinUnitInstanceFingerprint.method.definingClass

        // I tried to find the field name, but it's pretty obvious to me, so I hardcode it.
        // If it changes, we need to fix it
        AddOlkChatRoomListAdFingerprint.method.addInstructions(
            0,
            """
                sget-object v0, $unitClass->a:$unitClass
                return-object v0
            """.trimIndent()
        )

        OpenChatTabFragmentAdEnabledFingerprint.method.returnEarly(false)
    }
}