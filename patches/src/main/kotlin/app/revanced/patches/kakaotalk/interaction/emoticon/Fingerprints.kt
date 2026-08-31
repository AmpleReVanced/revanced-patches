package app.revanced.patches.kakaotalk.interaction.emoticon

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal const val EMOTICON_PLUS_ME_RESULT_CLASS =
    "Lcom/kakao/talk/emoticon/membership/data/remote/dto/EmoticonPlusMeResult;"

internal object EmoticonPlusMeResultConstructorFingerprint : Fingerprint(
    definingClass = EMOTICON_PLUS_ME_RESULT_CLASS,
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
    filters = listOf(
        methodCall(
            definingClass = "Lcom/kakao/talk/module/emoticon/contract/EmoticonModuleFacade;",
            name = "getPlusManager",
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        opcode(Opcode.INVOKE_INTERFACE, location = MatchAfterImmediately()),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
        opcode(Opcode.RETURN, location = MatchAfterImmediately()),
    ),
    custom = { _, classDef -> classDef.sourceFile == "UserPreferencesExt.kt" }
)

internal object EmoticonPlusManagerIsActiveFingerprint : Fingerprint(
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