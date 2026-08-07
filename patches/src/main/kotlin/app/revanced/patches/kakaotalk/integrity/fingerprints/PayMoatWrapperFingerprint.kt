package app.revanced.patches.kakaotalk.integrity.fingerprints

import app.morphe.patcher.Fingerprint

/**
 * The KakaoPay Moat wrapper class. Its `(String, Function3)` methods each dispatch the native Moat
 * scan, so the scan-dispatch method reference can be read out of one of them.
 */
internal object PayMoatWrapperFingerprint : Fingerprint(
    custom = { _, classDef ->
        classDef.sourceFile == "PayMoatWrapper.kt" &&
            classDef.methods.any { method ->
                method.returnType == "V" &&
                    method.parameterTypes.map { it.toString() } ==
                    listOf("Ljava/lang/String;", "Lkotlin/jvm/functions/Function3;")
            }
    },
)
