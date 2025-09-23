package app.revanced.patches.kakaotalk.ads.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val addOlkChatRoomListAdFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameters("Ljava/lang/Object;")
    returns("Ljava/lang/Object;")
    strings("call to \'resume\' before \'invoke\' with coroutine")
    opcodes(
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IGET,
        Opcode.CONST_4,
        Opcode.IF_EQZ,
        Opcode.IF_NE,
        Opcode.INVOKE_STATIC,
        Opcode.GOTO,
        Opcode.NEW_INSTANCE,
        Opcode.CONST_STRING,
    )
    custom { method, classDef -> classDef.sourceFile == "OlkChatRoomListViewModel.kt" }
}