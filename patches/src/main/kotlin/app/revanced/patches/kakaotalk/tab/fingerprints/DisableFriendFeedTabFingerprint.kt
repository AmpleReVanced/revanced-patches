package app.revanced.patches.kakaotalk.tab.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object MainTabConfigFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    parameters = listOf("Z", "Z", "Z", "Z", "Z", "Z", "Ljava/lang/String;"),
    returnType = "V",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_DIRECT,
        Opcode.IPUT_BOOLEAN,
        Opcode.IPUT_BOOLEAN,
        Opcode.IPUT_BOOLEAN,
        Opcode.IPUT_BOOLEAN,
        Opcode.IPUT_BOOLEAN,
        Opcode.IPUT_BOOLEAN,
        Opcode.IPUT_OBJECT,
        Opcode.RETURN_VOID,
    ),
    custom = { method, classDef -> classDef.sourceFile == "MainTabConfig.kt" }
)

internal object IsHideFriendsTabSettingsFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(),
    returnType = "Z",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.SGET_OBJECT,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT,
        Opcode.RETURN,
    ),
    custom = { method, classDef -> classDef.sourceFile == "FriendTab.kt" }
)

internal object DetermineFeedOrListMethodFingerprint : Fingerprint(
    name = "invokeSuspend",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Ljava/lang/Object;"),
    returnType = "Ljava/lang/Object;",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_STATIC,
        Opcode.IGET,
        Opcode.IF_NEZ,
        Opcode.INVOKE_STATIC,
        Opcode.IGET_BOOLEAN,
        Opcode.IGET_BOOLEAN,
        Opcode.IGET_BOOLEAN,
        Opcode.SGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.IF_EQZ,
        Opcode.NEW_INSTANCE,
        Opcode.INVOKE_DIRECT,
        Opcode.RETURN_OBJECT,
        Opcode.NEW_INSTANCE,
        Opcode.INVOKE_DIRECT,
        Opcode.RETURN_OBJECT,
        Opcode.NEW_INSTANCE,
        Opcode.CONST_STRING,
        Opcode.INVOKE_DIRECT,
        Opcode.THROW,
    ),
    custom = { method, classDef -> classDef.sourceFile == "FriendSettingsViewModel.kt" }
)