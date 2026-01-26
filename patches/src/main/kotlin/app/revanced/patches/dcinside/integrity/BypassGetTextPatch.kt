package app.revanced.patches.dcinside.integrity

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patches.dcinside.integrity.fingerprints.nativeGetTextFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

@Suppress("unused")
val bypassGetTextPath = bytecodePatch(
    name = "Bypass getText",
    description = "Bypasses the integrity check for getText.",
) {
    compatibleWith("com.dcinside.app.android"("5.2.5"))
    extendWith("extensions/dcinside.rve")

    execute {
        val nativeGetTextMethod = nativeGetTextFingerprint.method

        nativeGetTextFingerprint.classDef.methods.remove(nativeGetTextMethod)

        nativeGetTextFingerprint.classDef.methods.add(
            ImmutableMethod(
                nativeGetTextMethod.definingClass,
                "gt",
                listOf(
                    ImmutableMethodParameter("Landroid/content/Context;", null, null),
                    ImmutableMethodParameter("Ljava/lang/String;", null, null),
                    ImmutableMethodParameter("Ljava/lang/String;", null, null),
                    ImmutableMethodParameter("Ljava/lang/String;", null, null)
                ),
                "Ljava/lang/String;",
                nativeGetTextMethod.accessFlags and AccessFlags.NATIVE.value.inv(),
                null,
                null,
                MutableMethodImplementation(5)
            ).toMutable().apply {
                addInstructions(
                    """
                        invoke-static {p1, p2, p3, p4}, Lapp/revanced/extension/dcinside/api/AppId;->getAppId(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
                    
                        move-result-object v0
                        
                        return-object v0
                    """.trimIndent()
                )
            }
        )
    }
}