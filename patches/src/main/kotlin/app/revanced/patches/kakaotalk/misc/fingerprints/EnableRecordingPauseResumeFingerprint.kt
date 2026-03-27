package app.revanced.patches.kakaotalk.misc.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object IsRecordingPauseResumeEnabled : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(),
    strings = listOf("is_enable_recording_pause_resume_enabled"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CONST_STRING,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CONST_4
    ),
    custom = { method, classDef -> classDef.sourceFile == "CbtPreferences.kt" }
)