package app.revanced.patches.kakaotalk.integrity

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patches.kakaotalk.integrity.fingerprints.checkApkChecksumsFingerprint
import app.revanced.patches.kakaotalk.integrity.fingerprints.moatNativeStatusFingerprint
import app.revanced.patches.kakaotalk.misc.addExtensionPatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

@Suppress("unused")
val bypassMoatCheckPatch = bytecodePatch(
    name = "Bypass Moat check",
    description = "Bypass Moat check that prevents the app from running.",
    use = false,
) {
    compatibleWith("com.kakao.talk"("26.2.0"))
    dependsOn(addExtensionPatch)

    execute {
        checkApkChecksumsFingerprint.method.apply {
            println(this)
            addInstructions(
                0,
                """
                    new-instance v0, Lkotlin/Pair;
                    sget-object v1, Lui0/a;->VERIFIED:Lui0/a;
                    const-string v2, ""
                    invoke-direct {v0, v1, v2}, Lkotlin/Pair;-><init>(Ljava/lang/Object;Ljava/lang/Object;)V
                    return-object v0
                """.trimIndent()
            )
        }

        val nativeMethod = moatNativeStatusFingerprint.method
        moatNativeStatusFingerprint.classDef.methods.remove(nativeMethod)
        moatNativeStatusFingerprint.classDef.methods.add(
            ImmutableMethod(
                nativeMethod.definingClass,
                "a",
                listOf(
                    ImmutableMethodParameter("I", null, null),
                    ImmutableMethodParameter("I", null, null)
                ),
                "[Lk/a;",
                nativeMethod.accessFlags and AccessFlags.NATIVE.value.inv(),
                null,
                null,
                MutableMethodImplementation(5)
            ).toMutable().apply {
                addInstructions(
                    """
                        invoke-static {}, Lapp/revanced/extension/kakaotalk/integrity/Moat;->returnEmpty()[Lk/a;
                    
                        move-result-object v0
                        
                        return-object v0
                    """.trimIndent()
                )
            }
        )
    }
}