package app.revanced.patches.kakaotalk.ad.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object ChatListAdHelperEnabledFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Landroid/content/Context;"),
    returnType = "Z",
    filters = listOf(
        methodCall("Landroid/content/res/Resources;->getConfiguration()Landroid/content/res/Configuration;"),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        fieldAccess(
            "Landroid/content/res/Configuration;->orientation:I",
            location = MatchAfterImmediately(),
        ),
        opcode(Opcode.CONST_4, location = MatchAfterImmediately()),
        opcode(Opcode.IF_NE, location = MatchAfterImmediately()),
        opcode(Opcode.SGET_OBJECT, location = MatchAfterImmediately()),
        opcode(Opcode.INVOKE_VIRTUAL, location = MatchAfterImmediately()),
    ),
    custom = { _, classDef -> classDef.sourceFile == "ChatListAdHelper.kt" }
)

internal object ChatListGlobalAdEnabledFingerprint : Fingerprint(
    classFingerprint = ChatListAdHelperEnabledFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(
        "Landroid/content/Context;",
        "Z",
        "Lkotlin/coroutines/Continuation;",
    ),
    returnType = "Ljava/lang/Object;",
    filters = listOf(
        methodCall("Landroid/content/res/Resources;->getConfiguration()Landroid/content/res/Configuration;"),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        fieldAccess("Landroid/content/res/Configuration;->orientation:I"),
    ),
    custom = { _, classDef -> classDef.sourceFile == "ChatListAdHelper.kt" }
)
