package app.revanced.patches.kakaotalk.layout.tab.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

private object MainTabConfigToStringFingerprint : Fingerprint(
    name = "toString",
    strings = listOf(", isNowTabEnabled="),
)

internal object MainTabConfigFingerprint : Fingerprint(
    classFingerprint = MainTabConfigToStringFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
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
    ),
    custom = { method, classDef -> classDef.sourceFile == "MainTabConfig.kt" }
)

internal object DetermineFeedOrListMethodFingerprint : Fingerprint(
    name = "invokeSuspend",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Ljava/lang/Object;"),
    returnType = "Ljava/lang/Object;",
    filters = listOf(
        methodCall(parameters = listOf(), returnType = "Z"),
        methodCall(name = "<init>", parameters = listOf("Z", "Z", "Z"), returnType = "V"),
    ),
    custom = { _, classDef -> classDef.sourceFile == "FriendSettingsViewModel.kt" },
)