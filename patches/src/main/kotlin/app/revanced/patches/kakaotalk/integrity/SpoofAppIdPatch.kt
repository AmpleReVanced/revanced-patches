package app.revanced.patches.kakaotalk.integrity

import app.revanced.patcher.extensions.InstructionExtensions.replaceInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.integrity.fingerprints.getAppIdFingerprint
import app.revanced.patches.kakaotalk.integrity.fingerprints.uaffacetidMethodFingerprint

@Suppress("unused")
val spoofAppIdPatch = bytecodePatch(
    name = "Spoof App ID",
    description = "Spoofs the App ID to bypass integrity checks. (to bypass biometric auth etc.)",
) {
    compatibleWith("com.kakao.talk"("26.1.3"))

    execute {
        val apkKeyHash = "android:apk-key-hash:7MRbkCrB6DyL4XWKJX5nSS3jdFY"

        val method = getAppIdFingerprint.method
        method.replaceInstructions(
            0,
            """
                const-string v0, "$apkKeyHash"
                return-object v0
            """.trimIndent()
        )

        val uaffacetidClass = uaffacetidMethodFingerprint.classDef
        uaffacetidClass.methods.forEach {
            if (it.returnType != "Ljava/lang/String;") return@forEach
            it.replaceInstructions(
                0,
                """
                    const-string v0, "$apkKeyHash"
                    return-object v0
                """.trimIndent()
            )
        }
    }
}