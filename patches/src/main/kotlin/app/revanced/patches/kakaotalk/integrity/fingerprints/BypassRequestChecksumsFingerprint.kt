package app.revanced.patches.kakaotalk.integrity.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val requestChecksumsFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.BRIDGE, AccessFlags.SYNTHETIC)
    parameters("Landroid/content/pm/PackageManager;", "Ljava/lang/String;", "Z", "I", "Ljava/util/List;", "Landroid/content/pm/PackageManager\$OnChecksumsReadyListener;")
    returns("V")
    strings()
    opcodes(
        Opcode.INVOKE_VIRTUAL_RANGE,
        Opcode.RETURN_VOID,
    )
}