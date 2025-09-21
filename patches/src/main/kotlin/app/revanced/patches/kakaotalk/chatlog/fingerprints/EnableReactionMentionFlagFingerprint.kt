package app.revanced.patches.kakaotalk.chatlog.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val reactionMentionFlagFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("Ljava/lang/Object;")
    parameters("Ljava/lang/Object;")
    strings("reaction_mention_flag")
    opcodes(
        Opcode.SGET_OBJECT,
        Opcode.IGET,
        Opcode.CONST_4,
        Opcode.IF_EQZ,
        Opcode.IF_NE,
    )
    custom { method, classDef -> classDef.sourceFile == "CbtPreferencesWithBlocking.kt" }
}