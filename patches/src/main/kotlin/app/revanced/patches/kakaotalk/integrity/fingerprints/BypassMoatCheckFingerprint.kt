package app.revanced.patches.kakaotalk.integrity.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object CheckApkChecksumsFingerprint : Fingerprint(
    definingClass = "Lcom/kakaopay/kpsd/moat/sdk/",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(),
    returnType = "Lkotlin/Pair;",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.IF_EQZ,
        Opcode.MOVE_OBJECT,
        Opcode.CHECK_CAST,
    )
)

internal object MoatNativeStatusFingerprint : Fingerprint(
    name = "a", // Hard coded obfuscated method name.
    accessFlags = listOf(
        AccessFlags.PUBLIC,
        AccessFlags.STATIC,
        AccessFlags.FINAL,
        AccessFlags.NATIVE
    ),
    parameters = listOf("I", "I")
)

internal object MoatResultClassFingerprint : Fingerprint(
    name = "<init>",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    parameters = listOf(
        "Lcom/kakaopay/kpsd/moat/sdk/MoatFlag\$PrivateMoatFlag;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Z"
    ),
    returnType = "V",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_DIRECT,
        Opcode.IPUT_OBJECT,
        Opcode.IPUT_OBJECT,
        Opcode.IPUT_OBJECT,
        Opcode.IPUT_OBJECT,
        Opcode.IPUT_BOOLEAN,
        Opcode.RETURN_VOID
    )
)