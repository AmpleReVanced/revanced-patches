package app.revanced.patches.kakaotalk.misc.settings

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object MainSettingItemTypeFingerprint : Fingerprint(
    custom = { _, classDef ->
        classDef.sourceFile == "MainSettingItemType.kt"
                && classDef.superclass == "Ljava/lang/Enum;"
    }
)

internal object SetupSettingsItemFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Landroid/content/Context;", "Landroid/content/Intent;"),
    returnType = "Ljava/util/List;",
    filters = listOf(fieldAccess(name = "LABORATORY", opcode = Opcode.SGET_OBJECT)),
    custom = { _, classDef -> classDef.sourceFile == "SettingActivity.kt" },
)

internal object SettingsDividerFingerprint : Fingerprint(
    name = "<init>",
    parameters = List(5) { "I" },
    custom = { _, classDef -> classDef.sourceFile == "DividerItem.kt" },
)

internal object ThemePrefClassFingerprint : Fingerprint(
    custom = { _, classDef -> classDef.sourceFile == "ThemePref.kt" },
)

internal object ThemePrefNightModeReadFingerprint : Fingerprint(
    classFingerprint = ThemePrefClassFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(),
    returnType = "I",
    strings = listOf("night_mode"),
)

internal object ThemePrefNightModeWriteFingerprint : Fingerprint(
    classFingerprint = ThemePrefClassFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("I"),
    returnType = "V",
    strings = listOf("night_mode"),
)
