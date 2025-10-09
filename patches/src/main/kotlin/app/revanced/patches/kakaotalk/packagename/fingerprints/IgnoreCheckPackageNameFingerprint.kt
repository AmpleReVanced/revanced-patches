package app.revanced.patches.kakaotalk.packagename.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val checkPackageNameFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameters("Landroid/content/Context;")
    returns("V")
    strings("context", "com_kakao_talk", "_", ".", "Check failed.")
    opcodes(
        Opcode.CONST_STRING,
        Opcode.INVOKE_STATIC,
        Opcode.SGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.SGET_OBJECT,
        Opcode.IF_NE
    )
    custom { method, classDef -> classDef.sourceFile == "AppHelper.kt" }
}

internal val getInstallSourceInfoFingerprint = fingerprint {
    strings("com.kakao.talk")
    custom { method, classDef -> classDef.type == "Lcom/kakao/talk/application/initializer/Initializer\$a;" && method.name == "invokeSuspend" }
}