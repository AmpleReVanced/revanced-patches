package app.revanced.patches.kakaotalk.ads.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val measuringBizBoardFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC)
    returns("V")
    parameters("I", "I")
    opcodes(
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.INT_TO_FLOAT,
        Opcode.CONST,
        Opcode.MUL_FLOAT_2ADDR,
        Opcode.FLOAT_TO_INT,
        Opcode.IGET,
        Opcode.IF_LE,
        Opcode.MOVE,
        Opcode.CONST_HIGH16,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.INVOKE_SUPER,
        Opcode.RETURN_VOID
    )
}