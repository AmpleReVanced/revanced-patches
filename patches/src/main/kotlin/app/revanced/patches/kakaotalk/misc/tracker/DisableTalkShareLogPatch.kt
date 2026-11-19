package app.revanced.patches.kakaotalk.misc.tracker

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.revanced.patches.kakaotalk.misc.tracker.fingerprints.TalkShareApiFingerprint
import app.revanced.patches.kakaotalk.misc.tracker.fingerprints.TalkShareLogFlagFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO

@Suppress("unused")
val disableTalkShareLogPatch = patch(
    name = "Disable Talk Share Log",
    description = "Disable talk share log"
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        TalkShareLogFlagFingerprint.method.returnEarly(false)

        Fingerprint(
            returnType = "V",
            filters = listOf(methodCall(TalkShareApiFingerprint.originalMethod)),
        ).matchAll().forEach { it.method.returnEarly() }
    }
}
