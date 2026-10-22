package app.revanced.patches.kakaotalk.layout.tab.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.StringComparisonType.STARTS_WITH
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object ChatPaneStateFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    strings = listOf("ChatScreenPaneState(currentPane=", ", isListPaneHidden="),
    custom = { _, classDef -> classDef.sourceFile == "ChatScreenPaneState.kt" },
)

internal object ChatNavigationProgressFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf("L", "L", "L"),
    filters = listOf(
        methodCall(opcode = Opcode.INVOKE_STATIC, parameters = listOf("L"), returnType = "Z"),
        methodCall(opcode = Opcode.INVOKE_STATIC, parameters = listOf("L"), returnType = "Z"),
        methodCall(
            definingClass = "Lkotlin/jvm/internal/Intrinsics;",
            parameters = listOf("Ljava/lang/Object;", "Ljava/lang/Object;"),
            returnType = "Z",
        ),
    ),
    custom = { _, classDef -> classDef.sourceFile == "ChatScreen.kt" },
)

context(_: BytecodePatchContext)
internal fun chatDetailNavigationFingerprint() = Fingerprint(
    definingClass = ChatNavigationProgressFingerprint.originalClassDef.type,
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
)

context(_: BytecodePatchContext)
internal fun chatListPaneVisibilityFingerprint() = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(
        "L", "L", "L", "Z", "Lkotlin/jvm/functions/Function0;",
        "Lkotlin/jvm/functions/Function2;", "Lkotlin/jvm/functions/Function3;",
        "Lkotlin/jvm/functions/Function1;", "Lkotlin/jvm/functions/Function0;", "L", "I",
    ),
    strings = listOf(string("com.kakao.talk.activity.adaptive.main.tab.chat.ChatScreenScaffold (", STARTS_WITH)),
    filters = listOf(
        methodCall(
            definingClass = ChatPaneStateFingerprint.originalClassDef.type,
            parameters = listOf(),
            returnType = "Z",
        ),
    ),
    custom = { _, classDef -> classDef.sourceFile == "ChatScreen.kt" },
)

context(_: BytecodePatchContext)
internal fun chatNavigationVisibilityFingerprint() = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(
        "L", "L", "Z", "L", "L", "Lkotlin/jvm/functions/Function1;",
        "Lkotlin/jvm/functions/Function1;", "Lkotlin/jvm/functions/Function2;",
        "Lkotlin/jvm/functions/Function3;", "Lkotlin/jvm/functions/Function0;", "L", "I", "I",
    ),
    strings = listOf(string("com.kakao.talk.activity.adaptive.main.tab.chat.ChatScreen (", STARTS_WITH)),
    filters = listOf(
        methodCall(smali = ChatNavigationProgressFingerprint.originalMethod.toString()),
        methodCall(
            definingClass = ChatPaneStateFingerprint.originalClassDef.type,
            parameters = listOf(),
            returnType = "Z",
        ),
        methodCall(smali = "Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;"),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        opcode(Opcode.CONST_HIGH16, location = MatchAfterImmediately()),
    ),
    custom = { _, classDef -> classDef.sourceFile == "ChatScreen.kt" },
)
