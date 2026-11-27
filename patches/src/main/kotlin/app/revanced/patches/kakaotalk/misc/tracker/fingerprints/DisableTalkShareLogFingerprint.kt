package app.revanced.patches.kakaotalk.misc.tracker.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object TalkShareApiFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.ABSTRACT),
    parameters = listOf("L"),
    returnType = "L",
    custom = { _, classDef -> classDef.sourceFile == "TalkShareApi.kt" },
)

internal object TalkShareLogFlagFingerprint : Fingerprint(
    returnType = "Z",
    filters = listOf(fieldAccess(name = "USE_TALK_SHARE_LOG", opcode = Opcode.SGET_OBJECT)),
    custom = { _, classDef -> classDef.sourceFile == "AvailableFlags.kt" },
)
