package app.revanced.patches.kakaotalk.integrity

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patches.kakaotalk.integrity.fingerprints.checkApkChecksumsFingerprint
import app.revanced.patches.kakaotalk.integrity.fingerprints.moatNativeStatusFingerprint
import app.revanced.patches.kakaotalk.integrity.fingerprints.moatResultClassFingerprint
import app.revanced.util.getReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

@Suppress("unused")
val bypassMoatCheckPatch = bytecodePatch(
    name = "Bypass Moat check",
    description = "Bypass Moat check that prevents the app from running.",
    use = false,
) {
    compatibleWith("com.kakao.talk"("26.2.2"))

    execute {
        checkApkChecksumsFingerprint.method.apply {
            val lastSgetObjectType = instructions.last { it.opcode == Opcode.SGET_OBJECT }.getReference<FieldReference>()?.type

            addInstructions(
                0,
                """
                    new-instance v0, Lkotlin/Pair;
                    sget-object v1, $lastSgetObjectType->VERIFIED:$lastSgetObjectType
                    const-string v2, ""
                    invoke-direct {v0, v1, v2}, Lkotlin/Pair;-><init>(Ljava/lang/Object;Ljava/lang/Object;)V
                    return-object v0
                """.trimIndent()
            )
        }

        val moatResultClass = moatResultClassFingerprint.classDef

        val nativeMethod = moatNativeStatusFingerprint.method
        moatNativeStatusFingerprint.classDef.methods.remove(nativeMethod)
        moatNativeStatusFingerprint.classDef.methods.add(
            ImmutableMethod(
                nativeMethod.definingClass,
                nativeMethod.name,
                listOf(
                    ImmutableMethodParameter("I", null, null),
                    ImmutableMethodParameter("I", null, null)
                ),
                moatResultClass.type,
                nativeMethod.accessFlags and AccessFlags.NATIVE.value.inv(),
                null,
                null,
                MutableMethodImplementation(5)
            ).toMutable().apply {
                addInstructions(
                    """
                        const/4 v0, 0x0
                        new-array v0, v0, ${moatResultClass.type}
                        
                        return-object v0
                    """.trimIndent()
                )
            }
        )
    }
}