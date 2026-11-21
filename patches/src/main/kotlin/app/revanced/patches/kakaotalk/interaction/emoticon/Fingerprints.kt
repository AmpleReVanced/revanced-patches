package app.revanced.patches.kakaotalk.interaction.emoticon

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.OpcodesFilter
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

internal object EmoticonPlusMeResultIsActiveFingerprint : Fingerprint(
    definingClass = EMOTICON_PLUS_ME_RESULT_CLASS,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(),
    returnType = "Z",
    filters = listOf(
        opcode(Opcode.IGET_BOOLEAN),
        opcode(Opcode.RETURN, location = MatchAfterImmediately()),
    ),
)
