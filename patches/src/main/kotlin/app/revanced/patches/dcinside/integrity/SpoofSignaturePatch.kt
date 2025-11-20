package app.revanced.patches.dcinside.integrity

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patches.dcinside.integrity.fingerprints.nativeGetSignatureByTypeFingerprint
import app.revanced.patches.dcinside.integrity.fingerprints.nativeGetSignatureHexFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

@Suppress("unused")
val spoofSignaturePatch = bytecodePatch(
    name = "Spoof Signature",
    description = "Spoofs the app signature to bypass integrity checks.",
) {
    compatibleWith("com.dcinside.app.android"("5.2.1"))
    extendWith("extensions/dcinside.rve")

    execute {
        val nativeGetSignatureHexMethod = nativeGetSignatureHexFingerprint.method
        nativeGetSignatureHexFingerprint.classDef.methods.remove(nativeGetSignatureHexMethod)
        nativeGetSignatureHexFingerprint.classDef.methods.add(
            ImmutableMethod(
                nativeGetSignatureHexMethod.definingClass,
                nativeGetSignatureHexMethod.name,
                listOf(
                    ImmutableMethodParameter("Landroid/content/Context;", null, null),
                ),
                "Ljava/lang/String;",
                nativeGetSignatureHexMethod.accessFlags and AccessFlags.NATIVE.value.inv(),
                null,
                null,
                MutableMethodImplementation(5)
            ).toMutable().apply {
                addInstructions(
                    """
                        invoke-static {}, Lapp/revanced/extension/dcinside/api/AppId;->getApkSignatureHex()Ljava/lang/String;
                    
                        move-result-object v0
                        
                        return-object v0
                    """.trimIndent()
                )
            }
        )

        val nativeGetSignatureByTypeMethod = nativeGetSignatureByTypeFingerprint.method
        nativeGetSignatureByTypeFingerprint.classDef.methods.remove(nativeGetSignatureByTypeMethod)
        nativeGetSignatureByTypeFingerprint.classDef.methods.add(
            ImmutableMethod(
                nativeGetSignatureByTypeMethod.definingClass,
                nativeGetSignatureByTypeMethod.name,
                listOf(
                    ImmutableMethodParameter("Ljava/lang/String;", null, null)
                ),
                "Ljava/util/ArrayList;",
                nativeGetSignatureByTypeMethod.accessFlags and AccessFlags.NATIVE.value.inv(),
                null,
                null,
                MutableMethodImplementation(5)
            ).toMutable().apply {
                addInstructions(
                    """
                        invoke-static {p1}, Lapp/revanced/extension/dcinside/api/AppId;->getApkSignatureByType(Ljava/lang/String;)Ljava/util/ArrayList;
                    
                        move-result-object v0
                        
                        return-object v0
                    """.trimIndent()
                )
            }
        )
    }
}