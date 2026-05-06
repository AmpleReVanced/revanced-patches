package app.revanced.patches.dcinside.integrity.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

// sh
internal object NativeGetSignatureHexFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL, AccessFlags.NATIVE),
    parameters = listOf("Landroid/content/Context;"),
    returnType = "Ljava/lang/String;",
)

// vd
internal object NativeGetSignatureByTypeFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL, AccessFlags.NATIVE),
    parameters = listOf("Ljava/lang/String;"),
    returnType = "Ljava/util/ArrayList;",
)

internal object GenerateXAndroidCertFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.STATIC, AccessFlags.SYNTHETIC),
    returnType = "V",
    definingClass = "Lcom/dcinside/app/util",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_VIRTUAL,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CONST_16
    ),
    strings = listOf("SHA-1", "Package info not found.")
)