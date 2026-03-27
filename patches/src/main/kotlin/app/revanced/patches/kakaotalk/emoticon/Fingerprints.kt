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