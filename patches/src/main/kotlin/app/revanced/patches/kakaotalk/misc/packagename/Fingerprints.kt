package app.revanced.patches.kakaotalk.misc.packagename

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object CheckPackageNameFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Landroid/content/Context;"),
    returnType = "V",
    strings = listOf("com_kakao_talk", "_", ".", "Check failed."),
    filters = listOf(
        methodCall(
            definingClass = "Landroid/content/Context;",
            name = "getPackageName",
            parameters = listOf(),
            returnType = "Ljava/lang/String;",
            opcode = Opcode.INVOKE_VIRTUAL,
        ),
    ),
    custom = { _, classDef -> classDef.sourceFile == "AppHelper.kt" }
)

internal object GetInstallSourceInfoFingerprint : Fingerprint(
    name = "invokeSuspend",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Ljava/lang/Object;"),
    returnType = "Ljava/lang/Object;",
    strings = listOf("com.kakao.talk"),
    custom = { _, classDef ->
        classDef.sourceFile == "Initializer.kt" && classDef.type.contains("$")
    },
)