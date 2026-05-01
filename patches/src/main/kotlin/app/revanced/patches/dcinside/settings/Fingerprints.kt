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

internal object UserMemoRegisterFingerprint : Fingerprint(
    returnType = "Z",
    strings = listOf(
        "galleryId",
        "keyValues",
        "userValue",
        "memo",
    ),
    custom = { method, _ ->
        method.parameterTypes.size == 5 &&
            method.parameterTypes[1].toString() == "Ljava/lang/String;" &&
            method.parameterTypes[2].toString() == "Ljava/lang/String;" &&
            method.parameterTypes[3].toString() == "Z" &&
            method.parameterTypes[4].toString().startsWith("[")
    },
)

internal object UserMemoPresetOpenRealmFingerprint : Fingerprint(
    definingClass = "Lapp/revanced/extension/dcinside/settings/UserMemoPresetPatch;",
    name = "openDefaultRealm",
)

internal object UserMemoPresetNewPairArrayFingerprint : Fingerprint(
    definingClass = "Lapp/revanced/extension/dcinside/settings/UserMemoPresetPatch;",
    name = "newMemoPairArray",
)

internal object UserMemoPresetNewPairFingerprint : Fingerprint(
    definingClass = "Lapp/revanced/extension/dcinside/settings/UserMemoPresetPatch;",
    name = "newMemoPair",
)

internal object UserMemoPresetRegisterEntriesFingerprint : Fingerprint(
    definingClass = "Lapp/revanced/extension/dcinside/settings/UserMemoPresetPatch;",
    name = "registerEntriesWithApp",
)
