package app.revanced.patches.dcinside.settings

import app.morphe.patcher.patch.ResourcePatchContext
import app.morphe.patcher.patch.resourcePatch
import app.morphe.util.ResourceGroup
import app.morphe.util.asSequence
import app.morphe.util.copyResources
import app.morphe.util.inputStreamFromBundledResource
import app.revanced.patches.dcinside.shared.addDcInsideResources
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE
import org.w3c.dom.Document
import org.w3c.dom.Element

private const val SETTINGS_SHORTCUT_ID = "morphe_dcinside_settings"
private const val USER_MEMO_PRESET_RESOURCE = "user_memo_preset_1.txt"

internal val addSettingsResourcesPatch = resourcePatch(
    name = "Add settings resources",
    description = "Adds Morphe settings resources to the app.",
) {
    compatibleWith(COMPATIBILITY_DC_INSIDE)

    execute {
        addDcInsideResources()

        copyResources(
            "settings",
            ResourceGroup(
                "layout",
                "morphe_dcinside_settings.xml",
            ),
            ResourceGroup(
                "xml",
                "morphe_dcinside_settings_preferences.xml",
            ),
        )
        copyUserMemoPresetResource()

        document("res/layout/fragment_settings.xml").use { document ->
            document.addSettingsShortcut()
        }
    }
}

private fun ResourcePatchContext.copyUserMemoPresetResource() {
    val source = inputStreamFromBundledResource("dcinside", USER_MEMO_PRESET_RESOURCE)
        ?: error("Could not find bundled resource: dcinside/$USER_MEMO_PRESET_RESOURCE")
    val destination = this["res/raw/$USER_MEMO_PRESET_RESOURCE"]

    destination.parentFile?.mkdirs()
    source.use { input ->
        destination.outputStream().use { output ->
            input.copyTo(output)
        }
    }
}

private fun Document.addSettingsShortcut() {
    val existingShortcut = getElementsByTagName("LinearLayout")
        .asSequence()
        .mapNotNull { it as? Element }
        .any { it.getAttribute("android:id") == "@id/$SETTINGS_SHORTCUT_ID" }

    if (existingShortcut) return

    val template = getElementsByTagName("LinearLayout")
        .asSequence()
        .mapNotNull { it as? Element }
        .firstOrNull { it.getAttribute("android:id") == "@id/setting_write_setting" }
        ?: error("Could not find setting_write_setting in fragment_settings.xml")

    val insertBefore = getElementsByTagName("LinearLayout")
        .asSequence()
        .mapNotNull { it as? Element }
        .firstOrNull { it.getAttribute("android:id") == "@id/setting_backup" }
        ?: error("Could not find setting_backup in fragment_settings.xml")

    val shortcut = template.cloneNode(true) as Element
    shortcut.setAttribute("android:id", "@id/$SETTINGS_SHORTCUT_ID")
    shortcut.childNodes
        .asSequence()
        .mapNotNull { it as? Element }
        .firstOrNull { it.tagName == "TextView" }
        ?.setAttribute("android:text", "@string/morphe_label_for_ample_settings")

    insertBefore.parentNode.insertBefore(shortcut, insertBefore)
}
