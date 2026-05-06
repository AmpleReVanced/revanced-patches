package app.revanced.patches.dcinside.ads.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object DisableAdControllerFingerprint : Fingerprint(
    definingClass = "Lcom/dcinside/app/ad/support",
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    parameters = listOf(),
    returnType = "V",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.IGET_OBJECT,
        Opcode.IF_NEZ,
        Opcode.RETURN_VOID,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
    )
)