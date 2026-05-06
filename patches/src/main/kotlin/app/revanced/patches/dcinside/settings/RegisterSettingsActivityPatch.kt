package app.revanced.patches.dcinside.settings

import app.morphe.patcher.patch.resourcePatch
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE
import org.w3c.dom.Element

private const val SETTINGS_ACTIVITY = "app.revanced.extension.dcinside.settings.SettingsActivity"

internal val registerSettingsActivityPatch = resourcePatch(
    name = "Register settings activity",
    description = "Registers the Morphe settings activity in the app manifest.",
) {
    compatibleWith(COMPATIBILITY_DC_INSIDE)

    execute {
        document("AndroidManifest.xml").use { document ->
            val application = document.getElementsByTagName("application").item(0) as Element
            val activities = application.getElementsByTagName("activity")

            for (i in 0 until activities.length) {
                val activity = activities.item(i) as? Element ?: continue
                if (activity.getAttribute("android:name") == SETTINGS_ACTIVITY) {
                    return@use
                }
            }

            val activity = document.createElement("activity")
            activity.setAttribute("android:name", SETTINGS_ACTIVITY)
            activity.setAttribute("android:excludeFromRecents", "true")
            activity.setAttribute("android:exported", "false")
            activity.setAttribute("android:label", "@string/morphe_label_for_ample_settings")
            activity.setAttribute("android:launchMode", "singleTop")
            activity.setAttribute("android:theme", "@android:style/Theme.DeviceDefault")
            application.appendChild(activity)
        }
    }
}
