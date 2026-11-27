package app.revanced.patches.kakaotalk.layout.tab.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.StringComparisonType.STARTS_WITH
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object ChatPaneStateFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    strings = listOf("ChatScreenPaneState(currentPane=", ", isDetailPaneActive="),
    custom = { _, classDef -> classDef.sourceFile == "ChatScreenPaneState.kt" },
)

private object MainNavigationBarScaffoldFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf(string("com.kakao.talk.activity.adaptive.navigation.MainNavigationBarScaffold (", STARTS_WITH)),
    custom = { _, classDef -> classDef.sourceFile == "MainNavigationScaffold.kt" },
)

internal fun chatListPaneFingerprint() = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    parameters = listOf(
        ChatPaneStateFingerprint.originalClassDef.type, "Lkotlin/jvm/functions/Function4;",
        "Lkotlin/jvm/functions/Function1;", "L", "L", "I",
    ),
    returnType = "Lkotlin/Unit;",
    filters = listOf(methodCall(reference = { MainNavigationBarScaffoldFingerprint.originalMethod })),
    custom = { _, classDef -> classDef.sourceFile == "ChatScreen.kt" },
)

internal fun chatDetailNavigationFingerprint() = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/Object;",
    parameters = listOf(
        "L", "L", "L", "Lkotlin/jvm/functions/Function1;",
        "Lkotlin/jvm/functions/Function1;", "Lkotlin/jvm/functions/Function1;",
        "L", "Z", "Lkotlin/coroutines/Continuation;",
    ),
    filters = listOf(
        methodCall(
            definingClass = ChatPaneStateFingerprint.originalClassDef.type,
            parameters = listOf(),
            returnType = "Z",
            opcode = Opcode.INVOKE_VIRTUAL,
        ),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
        opcode(Opcode.IF_NEZ, location = MatchAfterImmediately()),
        opcode(Opcode.CONST_4, location = MatchAfterImmediately()),
        methodCall(
            definingClass = "Lkotlin/coroutines/jvm/internal/Boxing;",
            parameters = listOf("Z"),
            returnType = "Ljava/lang/Boolean;",
            location = MatchAfterImmediately(),
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        methodCall(
            smali = "Lkotlin/jvm/functions/Function1;->invoke(Ljava/lang/Object;)Ljava/lang/Object;",
            location = MatchAfterImmediately(),
        ),
    ),
    custom = { _, classDef -> classDef.sourceFile == "ChatScreen.kt" },
)
