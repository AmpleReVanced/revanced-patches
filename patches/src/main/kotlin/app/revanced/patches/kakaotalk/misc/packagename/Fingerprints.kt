package app.revanced.patches.kakaotalk.misc.packagename

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
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

internal object EmoticonDecryptContextFingerprint : Fingerprint(
    name = "<clinit>",
    filters = listOf(
        methodCall("Landroid/content/Context;->getApplicationContext()Landroid/content/Context;"),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        fieldAccess(
            type = "Landroid/content/Context;",
            opcode = Opcode.SPUT_OBJECT,
            location = MatchAfterImmediately(),
        ),
    ),
    custom = { _, classDef -> classDef.sourceFile == "EmoticonDecrypt.kt" },
)