package app.revanced.patches.dcinside.onestore.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val applicationConfigClassFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
    parameters("Landroid/content/Context;")
    returns("V")
    strings("android.settings.APPLICATION_DETAILS_SETTINGS", "android.intent.category.DEFAULT", "package:")
    opcodes(
        Opcode.IF_NEZ,
        Opcode.RETURN_VOID,
        Opcode.NEW_INSTANCE,
        Opcode.INVOKE_DIRECT,
    )
}