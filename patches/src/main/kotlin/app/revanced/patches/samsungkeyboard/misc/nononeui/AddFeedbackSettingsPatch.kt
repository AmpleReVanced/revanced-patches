package app.revanced.patches.samsungkeyboard.misc.nononeui

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.Document
import app.morphe.util.asSequence
import app.morphe.util.getNode
import app.revanced.patches.samsungkeyboard.shared.Constants.COMPATIBILITY_SAMSUNG_KEYBOARD
import app.revanced.patches.samsungkeyboard.shared.addSamsungKeyboardResources
import org.w3c.dom.Element
import org.w3c.dom.NodeList

private const val ACTIVITY = "app.revanced.extension.samsungkeyboard.FeedbackSettingsActivity"
private const val CATEGORY = "SETTINGS_CATEGORY_CUSTOMIZATION_GESTURE_AND_FEEDBACK"
private const val PREFERENCE = "morphe_samsung_keyboard_feedback"

internal val addFeedbackSettingsPatch = resourcePatch {
    compatibleWith(COMPATIBILITY_SAMSUNG_KEYBOARD)

    execute {
        addSamsungKeyboardResources()

        document("AndroidManifest.xml").use { document ->
            document.registerFeedbackSettingsActivity()
        }
        document("res/xml/settings_main_layout.xml").use { document ->
            document.addFeedbackSettingsPreference()
        }
    }
}

private fun Document.registerFeedbackSettingsActivity() {
    val application = getNode("application") as Element
    if (application.getElementsByTagName("activity").hasAttribute("android:name", ACTIVITY)) return

    application.appendChild(createElement("activity").apply {
        setAttribute("android:name", ACTIVITY)
        setAttribute("android:excludeFromRecents", "true")
        setAttribute("android:exported", "false")
        setAttribute("android:label", "@string/morphe_samsung_keyboard_feedback_title")
        setAttribute("android:launchMode", "singleTop")
        setAttribute("android:theme", "@android:style/Theme.DeviceDefault.NoActionBar")
    })
}

private fun Document.addFeedbackSettingsPreference() {
    if (getElementsByTagName("Preference").hasAttribute("android:key", PREFERENCE)) return

    val category = getElementsByTagName("PreferenceCategory")
        .asSequence()
        .filterIsInstance<Element>()
        .first { it.getAttribute("android:key") == CATEGORY }
    val preference = createElement("Preference").apply {
        setAttribute("android:key", PREFERENCE)
        setAttribute("android:title", "@string/morphe_samsung_keyboard_feedback_title")
        setAttribute("android:summary", "@string/morphe_samsung_keyboard_feedback_summary")
        appendChild(createElement("intent").apply {
            setAttribute("android:action", "android.intent.action.MAIN")
            setAttribute("android:targetClass", ACTIVITY)
            setAttribute("android:targetPackage", "com.samsung.android.honeyboard")
        })
    }
    category.appendChild(preference)
}

private fun NodeList.hasAttribute(name: String, value: String) =
    asSequence().filterIsInstance<Element>().any { it.getAttribute(name) == value }