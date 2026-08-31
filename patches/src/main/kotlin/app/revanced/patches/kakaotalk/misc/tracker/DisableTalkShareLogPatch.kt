package app.revanced.patches.kakaotalk.misc.tracker

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.findMutableMethodOf
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.getReference
import app.morphe.util.returnEarly
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.patches.kakaotalk.misc.tracker.fingerprints.TalkShareApiFingerprint
import app.revanced.patches.kakaotalk.misc.tracker.fingerprints.TalkShareLogAsyncFlagFingerprint
import app.revanced.util.matches
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val disableTalkShareLogPatch = bytecodePatch(
    name = "Disable Talk Share Log",
    description = "Disable talk share log"
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        val flagMethod = TalkShareLogAsyncFlagFingerprint.method
        val flagRegister = flagMethod.getFreeRegisterProvider(0, 1).getFreeRegister4Bit()
        flagMethod.addInstructions(
            0,
            """
                sget-object v$flagRegister, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
                return-object v$flagRegister
            """.trimIndent()
        )

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
