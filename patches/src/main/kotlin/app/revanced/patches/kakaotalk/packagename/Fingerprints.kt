package app.revanced.patches.kakaotalk.packagename

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object CheckPackageNameFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Landroid/content/Context;"),
    returnType = "V",
    strings = listOf("context", "com_kakao_talk", "_", ".", "Check failed."),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.CONST_STRING,
        Opcode.INVOKE_STATIC,
        Opcode.SGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.SGET_OBJECT,
        Opcode.IF_NE
    ),
    custom = { _, classDef -> classDef.sourceFile == "AppHelper.kt" }
)

internal object GetInstallSourceInfoFingerprint : Fingerprint(
//    definingClass = "Lcom/kakao/talk/application/initializer/Initializer\$a;",
    definingClass = "Lcom/kakao/talk/application/initializer/Initializer",
    name = "invokeSuspend",
    strings = listOf("com.kakao.talk"),
)