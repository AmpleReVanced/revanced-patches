package app.revanced.patches.dcinside.ads.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val disableAdControllerFingerprint = fingerprint {
    accessFlags(AccessFlags.PRIVATE, AccessFlags.FINAL)
    parameters()
    returns("V")
    opcodes(
        Opcode.IGET_OBJECT,
        Opcode.IF_NEZ,
        Opcode.RETURN_VOID,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
    )
    custom { method, classDef -> classDef.toString().startsWith("Lcom/dcinside/app/ad/support") }
}