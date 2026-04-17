package app.revanced.patches.kakaotalk.tracker

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.findMutableMethodOf
import app.morphe.util.hasMethodCall
import app.morphe.util.returnEarly
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.patches.kakaotalk.tracker.fingerprints.TalkShareLogAsyncFlagFingerprint

@Suppress("unused")
val disableTalkShareLogPatch = bytecodePatch(
    name = "Disable Talk Share Log",
    description = "Disable talk share log"
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        TalkShareLogAsyncFlagFingerprint.method.addInstructions(
            0,
            """
                sget-object v0, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
                return-object v0
            """.trimIndent()
        )

        buildMap {
            classDefForEach { classDef ->
                val talkShareMethods = classDef.methods.filter { method ->
                    method.returnType == "V" &&
                            method.hasMethodCall(
                                "Lcom/kakao/talk/net/retrofit/service/B;",
                                "a",
                                "Lretrofit2/d;"
                            ) &&
                            method.hasMethodCall(
                                "Lretrofit2/d;",
                                "g0",
                                "V"
                            )
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
