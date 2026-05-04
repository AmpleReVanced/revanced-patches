package app.revanced.patches.dcinside.dccon

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object PostDcconImageHandlerFingerprint : Fingerprint(
    definingClass = "Lcom/dcinside/app/wv",
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.STATIC, AccessFlags.FINAL),
    parameters = listOf("L", "L"),
    returnType = "V",
    strings = listOf(
        "dccon",
        "dccondetail",
        "groupIndex",
    ),
)

internal object ReplyDcconBindFingerprint : Fingerprint(
    definingClass = "Lcom/dcinside/app/read/holder",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("L", "Z"),
    returnType = "V",
    strings = listOf("dcconPack"),
)
