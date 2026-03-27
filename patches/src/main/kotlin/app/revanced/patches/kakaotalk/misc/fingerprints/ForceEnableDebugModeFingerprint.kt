package app.revanced.patches.kakaotalk.misc.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object ConfigConstructorFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.STATIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
    parameters = listOf(),
    strings = listOf("agit.daumkakao.com", "agit.io", "agit.in", "play.google.com"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.NEW_INSTANCE,
        Opcode.INVOKE_DIRECT,
        Opcode.SPUT_OBJECT,
        Opcode.SGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.SGET_OBJECT,
        Opcode.CONST_4,
        Opcode.IF_EQ,
        Opcode.MOVE,
        Opcode.GOTO,
    ),
    custom = { _, classDef -> classDef.sourceFile == "Config.kt" }
)