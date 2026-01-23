package app.revanced.patches.kakaotalk.tab.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val setupAdapterFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameters()
    returns("V")
    opcodes(
        Opcode.MOVE_OBJECT_FROM16,
        Opcode.CONST_4,
        Opcode.IGET_OBJECT,
        Opcode.CONST_STRING,
        Opcode.CONST_4,
        Opcode.IF_NEZ,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
    )
    strings("binding", "getContext(...)")
    custom { method, classDef -> classDef.sourceFile == "OpenChatTabFragment.kt" }
}

internal val initViewModelFingerprint = fingerprint {
    custom { method, classDef -> classDef.sourceFile == "OpenChatTabFragment.kt" && method.name == "initViewModel" }
}

internal val commonChatRoomListAdapterClassFingerprint = fingerprint {
    custom { method, classDef -> classDef.sourceFile == "CommonChatRoomListAdapter.kt" && !classDef.type.contains("$") }
}