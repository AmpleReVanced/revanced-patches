package app.revanced.patches.kakaotalk.tracker.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.Opcode

internal val talkShareServiceInit = fingerprint {
    opcodes(
        Opcode.SGET_OBJECT,
        Opcode.SPUT_OBJECT,
        Opcode.SGET_OBJECT,
        Opcode.NEW_INSTANCE,
        Opcode.INVOKE_DIRECT,
        Opcode.CONST_STRING,
        Opcode.INVOKE_VIRTUAL,
        Opcode.INVOKE_VIRTUAL,
    )
    custom { method, classDef -> classDef.sourceFile == "TalkShareService.kt" }
}