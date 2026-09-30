package app.revanced.patches.kakaotalk.layout.tab

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.kakaotalk.misc.settings.PreferenceScreen
import app.revanced.patches.kakaotalk.misc.settings.addSettingsTabPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO

@Suppress("unused")
val removeShopTabPatch = bytecodePatch(
    name = "Remove shop tab",
    description = "Adds an option to remove the shop tab from the bottom navigation bar.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(addSettingsTabPatch)

    execute {
        PreferenceScreen.NAVIGATION.addPreferences(
            SwitchPreference(
                key = "morphe_pref_remove_shop_tab",
                titleKey = "morphe_settings_catalog_remove_shop_tab",
                summary = true,
            ),
        )
        setExtensionIsPatchIncluded("Lapp/revanced/extension/kakaotalk/patches/RemoveShopTabPatch;")

        removeMainTab("SHOPPING_TAB", "removeShopTab")
    }
}
