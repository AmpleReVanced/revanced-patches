package app.morphe.patches.shared.misc.settings.preference

import app.morphe.patches.util.resource.BaseResource
import org.w3c.dom.Document

class ActionIntentPreference(
    key: String? = null,
    titleKey: String = "${key}_title",
    summaryKey: String? = "${key}_summary",
    icon: String? = null,
    iconBold: String? = null,
    layout: String? = null,
    tag: String = "Preference",
    val intent: Intent,
) : BasePreference(key, titleKey, summaryKey, icon, iconBold, layout, tag) {
    override fun serialize(ownerDocument: Document, resourceCallback: (BaseResource) -> Unit) =
        super.serialize(ownerDocument, resourceCallback).apply {
            appendChild(ownerDocument.createElement("intent").also { intentNode ->
                intent.action?.let { intentNode.setAttribute("android:action", it) }
                intent.data?.let { intentNode.setAttribute("android:data", it) }
            })
        }

    data class Intent(
        val action: String? = null,
        val data: String? = null,
    )
}
