package app.revanced.patches.chzzk.cheatkey

import app.morphe.patcher.patch.bytecodePatch
import app.revanced.patches.chzzk.common.utils.forceBooleanGetterTrue
import app.revanced.patches.chzzk.shared.Constants.COMPATIBILITY_CHZZK

@Suppress("unused")
val unlockCheatKeyPatch = bytecodePatch(
    name = "Unlock cheat key",
    description = "Unlocks CHZZK cheat key subscription benefits without an active subscription.",
) {
    compatibleWith(COMPATIBILITY_CHZZK)

    execute {
        CheatKeyStatusFingerprint.classDef.forceBooleanGetterTrue("subscribing")
        StreamingChannelMyInfoFingerprint.classDef.forceBooleanGetterTrue("cheatKey")
    }
}