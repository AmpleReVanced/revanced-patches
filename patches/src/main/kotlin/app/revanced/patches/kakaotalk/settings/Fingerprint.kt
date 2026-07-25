package app.revanced.patches.kakaotalk.settings

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

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
