package app.revanced.patches.dcinside.misc

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object DisableUpdateCheckFingerprint : Fingerprint(
    definingClass = "Lcom/dcinside/app/main",
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    parameters = listOf("Ljava/lang/String;"),
    strings = listOf("null cannot be cast to non-null type androidx.appcompat.app.AppCompatActivity"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.SGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CONST_STRING,
        Opcode.INVOKE_STATIC,
        Opcode.CHECK_CAST,
        Opcode.INVOKE_VIRTUAL
    )
)