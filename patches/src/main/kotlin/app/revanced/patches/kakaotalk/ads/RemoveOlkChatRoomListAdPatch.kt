package app.revanced.patches.kakaotalk.ads

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.ads.fingerprints.addOlkChatRoomListAdFingerprint
import app.revanced.patches.kakaotalk.common.fingerprints.kotlinUnitInstanceFingerprint

@Suppress("unused")
val removeOlkChatRoomListAdPatch = bytecodePatch(
    name = "Remove OpenLink chat room list ad",
    description = "Removes the OpenLink chat room list ad.",
) {
    compatibleWith("com.kakao.talk"("25.7.3"))

    execute {
        val findUnit = kotlinUnitInstanceFingerprint.method
        val unitClass = findUnit.definingClass

        val method = addOlkChatRoomListAdFingerprint.method

        // I tried to find the field name, but it's pretty obvious to me, so I hardcode it.
        // If it changes, we need to fix it
        method.addInstructions(
            0,
            """
                sget-object v0, $unitClass->a:$unitClass
                return-object v0
            """.trimIndent()
        )
    }
}