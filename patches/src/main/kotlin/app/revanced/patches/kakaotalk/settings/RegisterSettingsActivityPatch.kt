package app.revanced.patches.kakaotalk.settings

import app.morphe.patcher.patch.resourcePatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import org.w3c.dom.Element

@Suppress("unused")
val registerSettingsActivityPatch = resourcePatch(
    name = "Register settings activity",
    description = "Registers the Morphe settings activity in the app manifest.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        document("AndroidManifest.xml").use { document ->
            val application = document.getElementsByTagName("application").item(0) as Element
            val activities = application.getElementsByTagName("activity")

            for (i in 0 until activities.length) {
                val activity = activities.item(i) as? Element ?: continue
                if (activity.getAttribute("android:name") == "app.revanced.extension.kakaotalk.settings.SettingsActivity") {
                    return@use
                }
            }

            val activity = document.createElement("activity")
            activity.setAttribute("android:name", "app.revanced.extension.kakaotalk.settings.SettingsActivity")
            activity.setAttribute("android:excludeFromRecents", "true")
            activity.setAttribute("android:exported", "false")
            activity.setAttribute("android:label", "@string/morphe_label_for_ample_settings")
            activity.setAttribute("android:launchMode", "singleTop")
            activity.setAttribute("android:theme", "@android:style/Theme.DeviceDefault")
            application.appendChild(activity)
        }
    }
}
