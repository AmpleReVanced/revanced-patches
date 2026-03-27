package app.revanced.patches.dcinside.ads.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object HideMainAdFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    parameters = listOf("Landroid/view/View;"),
    returnType = "V",
    strings = listOf("itemView"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.CONST_STRING,
        Opcode.INVOKE_STATIC,
        Opcode.INVOKE_DIRECT,
        Opcode.NEW_INSTANCE,
        Opcode.INVOKE_DIRECT,
        Opcode.MOVE_RESULT_OBJECT,
    )
)