package app.revanced.patches.kakaotalk.settings

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object MainSettingItemTypeFingerprint : Fingerprint(
    custom = { _, classDef ->
        classDef.sourceFile == "MainSettingItemType.kt"
                && classDef.superclass == "Ljava/lang/Enum;"
    }
)

internal object SetupSettingsItemFingerprint : Fingerprint(
    definingClass = "Lcom/kakao/talk/activity/setting/SettingActivity\$a;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Landroid/content/Context;", "Landroid/content/Intent;"),
    returnType = "Ljava/util/List;",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.MOVE_OBJECT_FROM16,
        Opcode.MOVE_OBJECT_FROM16,
        Opcode.CONST_STRING,
        Opcode.INVOKE_STATIC,
        Opcode.NEW_INSTANCE,
        Opcode.INVOKE_DIRECT
    )
)

internal object ThemePrefNightModeReadFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(),
    returnType = "I",
    strings = listOf("night_mode"),
    custom = { _, classDef -> classDef.sourceFile == "ThemePref.kt" }
)

internal object ThemePrefNightModeWriteFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("I"),
    returnType = "V",
    strings = listOf("night_mode"),
    custom = { _, classDef -> classDef.sourceFile == "ThemePref.kt" }
)
