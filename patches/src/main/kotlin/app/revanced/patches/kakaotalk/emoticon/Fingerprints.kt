package app.revanced.patches.kakaotalk.emoticon

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object EmoticonPlusMeResultConstructorFingerprint : Fingerprint(
    definingClass = "Lcom/kakao/talk/emoticon/itemstore/plus/EmoticonPlusMeResult;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    parameters = listOf("Z", "J", "J"),
    returnType = "V",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_DIRECT,
        Opcode.IPUT_BOOLEAN,
        Opcode.IPUT_WIDE,
        Opcode.IPUT_WIDE,
        Opcode.RETURN_VOID
    )
)

internal object UserPreferenceIsActiveEmoticonPlusFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "Z",
    strings = listOf("<this>"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.CONST_STRING,
        Opcode.INVOKE_STATIC,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT,
        Opcode.RETURN,
    ),
    custom = { _, classDef -> classDef.sourceFile == "UserPreferencesExt.kt" }
)

internal object EmoticonPlusManagerIsActiveFingerprint : Fingerprint(
    definingClass = "Lcom/kakao/talk/emoticon/itemstore/plus",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.SGET_OBJECT,
        Opcode.CONST_4,
        Opcode.IF_EQZ,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.CONST_4,
        Opcode.IF_NE,
        Opcode.RETURN,
        Opcode.RETURN,
    ),
    custom = { _, classDef -> classDef.sourceFile == "EmoticonPlusManager.kt" }
)