package app.revanced.patches.kakaotalk.misc.tracker

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.findMutableMethodOf
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.returnBoxedBooleanEarly
import app.morphe.patcher.util.returnEarly
import app.revanced.patches.kakaotalk.misc.tracker.fingerprints.TalkShareApiFingerprint
import app.revanced.patches.kakaotalk.misc.tracker.fingerprints.TalkShareLogAsyncFlagFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.util.matches
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val disableTalkShareLogPatch = bytecodePatch(
    name = "Disable Talk Share Log",
    description = "Disable talk share log"
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        TalkShareLogAsyncFlagFingerprint.method.returnBoxedBooleanEarly(false)

        val shareLogMethod = TalkShareApiFingerprint.method
        buildMap {
            classDefForEach { classDef ->
                val talkShareMethods = classDef.methods.filter { method ->
                    method.returnType == "V" &&
                            method.implementation?.instructions?.any { instruction ->
                                instruction.getReference<MethodReference>()?.matches(shareLogMethod) == true
                            } == true
                }

                if (talkShareMethods.isNotEmpty()) {
                    put(classDef, talkShareMethods)
                }
            }
        }.forEach { (classDef, methods) ->
            val mutableClass = mutableClassDefBy(classDef)
            methods.map(mutableClass::findMutableMethodOf).forEach { method ->
                method.returnEarly()
            }
        }
    }
}
