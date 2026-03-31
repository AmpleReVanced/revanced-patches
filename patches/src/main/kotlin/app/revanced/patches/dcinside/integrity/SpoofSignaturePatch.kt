package app.revanced.patches.dcinside.integrity

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patches.dcinside.integrity.fingerprints.NativeGetSignatureByTypeFingerprint
import app.revanced.patches.dcinside.integrity.fingerprints.NativeGetSignatureHexFingerprint
import app.revanced.patches.dcinside.misc.addExtensionPatch
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

@Suppress("unused")
val spoofSignaturePatch = bytecodePatch(
    name = "Spoof Signature",
    description = "Spoofs the app signature to bypass integrity checks.",
) {
    compatibleWith(COMPATIBILITY_DC_INSIDE)
    dependsOn(addExtensionPatch)

    execute {
        val nativeGetSignatureHexMethod = NativeGetSignatureHexFingerprint.method
        NativeGetSignatureHexFingerprint.classDef.methods.remove(nativeGetSignatureHexMethod)
        NativeGetSignatureHexFingerprint.classDef.methods.add(
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
                    0,
                    """
                        invoke-static {}, Lapp/revanced/extension/dcinside/api/AppId;->getApkSignatureHex()Ljava/lang/String;
                    
                        move-result-object v0
                        
                        return-object v0
                    """.trimIndent()
                )
            }
        )

        val nativeGetSignatureByTypeMethod = NativeGetSignatureByTypeFingerprint.method
        NativeGetSignatureByTypeFingerprint.classDef.methods.remove(nativeGetSignatureByTypeMethod)
        NativeGetSignatureByTypeFingerprint.classDef.methods.add(
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
                    0,
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