package app.revanced.patches.kakaotalk.misc.integrity.fingerprints

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object AdFitRootCheckFingerprint : Fingerprint(
    returnType = "Z",
    strings = listOf("/system/xbin/su", "/system/bin/su"),
    custom = { _, classDef -> classDef.type.startsWith("Lcom/kakao/adfit/") },
)

internal object AdFitEmulatorCheckFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    parameters = listOf(),
    returnType = "Z",
    strings = listOf("goldfish", "ranchu", "Genymotion"),
    custom = { _, classDef -> classDef.type.startsWith("Lcom/kakao/adfit/") },
)