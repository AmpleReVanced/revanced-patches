package app.revanced.patches.kakaotalk.send

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.util.findFreeRegister
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.kakaotalk.send.fingerprints.StripInvalidCharFormatPredicateFingerprint
import app.revanced.patches.kakaotalk.send.fingerprints.StripInvalidCharFormatSanitizerFingerprint
import app.revanced.patches.kakaotalk.settings.PreferenceScreen
import app.revanced.patches.kakaotalk.settings.addSettingsTabPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO

private const val EXTENSION_CLASS =
    "Lapp/revanced/extension/kakaotalk/patches/AllowInvisibleCharactersPatch;"

private const val ALLOW_INVISIBLE_CHARACTERS =
    "Lapp/revanced/extension/kakaotalk/settings/Settings;->allowInvisibleCharacters()Z"

@Suppress("unused")
val allowInvisibleCharactersPatch = bytecodePatch(
    name = "Allow invisible characters",
    description = "Stops zero width and other invisible characters from being stripped out of the " +
            "chat input and out of the messages shown in a chat room.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(addSettingsTabPatch)

    execute {
        PreferenceScreen.CHAT.addPreferences(
            SwitchPreference(
                key = "morphe_pref_allow_invisible_characters",
                titleKey = "morphe_settings_patch_allow_invisible_characters",
                summary = true,
            ),
        )
        setExtensionIsPatchIncluded(EXTENSION_CLASS)

        // A null return value tells the input filter contract to keep the typed text as is.
        StripInvalidCharFormatPredicateFingerprint.classDef.methods
            .first { it.name == "filter" }
            .apply {
                val free = getFreeRegisterProvider(0, 1).getFreeRegister4Bit()

                addInstructionsWithLabels(
                    0,
                    """
                        invoke-static {}, $ALLOW_INVISIBLE_CHARACTERS
                        move-result v$free
                        if-eqz v$free, :morphe_original
                        const/4 v$free, 0x0
                        return-object v$free
                        :morphe_original
                        nop
                    """
                )
            }

        StripInvalidCharFormatSanitizerFingerprint.method.apply {
            val free = findFreeRegister(0)

            addInstructionsWithLabels(
                0,
                """
                    invoke-static {}, $ALLOW_INVISIBLE_CHARACTERS
                    move-result v$free
                    if-eqz v$free, :morphe_original
                    return-object p0
                    :morphe_original
                    nop
                """
            )
        }
    }
}