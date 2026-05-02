package app.revanced.patches.dcinside.settings

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object SettingsFragmentOnViewCreatedFingerprint : Fingerprint(
    definingClass = "Lcom/dcinside/app/settings",
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = listOf("Landroid/view/View;", "Landroid/os/Bundle;"),
    returnType = "V",
    strings = listOf(
        "settingScreenResolution",
        "settingScreenDesc",
    ),
)
