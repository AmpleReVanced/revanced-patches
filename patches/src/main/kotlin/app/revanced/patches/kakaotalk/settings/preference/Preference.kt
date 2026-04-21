package app.revanced.patches.kakaotalk.settings.preference

import org.w3c.dom.Document
import org.w3c.dom.Element

internal abstract class BasePreference(
    private val key: String? = null,
    private val titleKey: String? = "${key}_title",
    private val summaryKey: String? = "${key}_summary",
    private val icon: String? = null,
    private val layout: String? = null,
    private val selectable: Boolean? = null,
    private val persistent: Boolean? = null,
    private val dependency: String? = null,
    private val tag: String,
) {
    open fun serialize(document: Document): Element =
        document.createElement(tag).apply {
            key?.let { setAttribute("android:key", it) }
            titleKey?.let { setAttribute("android:title", "@string/$it") }
            summaryKey?.let { setAttribute("android:summary", "@string/$it") }
            icon?.let { setAttribute("android:icon", it) }
            layout?.let { setAttribute("android:layout", it) }
            selectable?.let { setAttribute("android:selectable", it.toString()) }
            persistent?.let { setAttribute("android:persistent", it.toString()) }
            dependency?.let { setAttribute("android:dependency", it) }
        }
}

internal class PreferenceCategory(
    key: String,
    titleKey: String,
    private val preferences: Set<BasePreference>,
) : BasePreference(
    key = key,
    titleKey = titleKey,
    summaryKey = null,
    tag = "PreferenceCategory",
) {
    override fun serialize(document: Document): Element =
        super.serialize(document).apply {
            preferences.forEach { appendChild(it.serialize(document)) }
        }
}

internal class SwitchPreference(
    key: String,
    titleKey: String,
    summaryKey: String,
    defaultValue: Boolean,
    dependency: String? = null,
) : BasePreference(
    key = key,
    titleKey = titleKey,
    summaryKey = summaryKey,
    persistent = false,
    dependency = dependency,
    tag = "SwitchPreference",
) {
    private val defaultValue = defaultValue.toString()

    override fun serialize(document: Document): Element =
        super.serialize(document).apply {
            setAttribute("android:defaultValue", defaultValue)
        }
}

internal class TextPreference(
    key: String,
    titleKey: String,
    summaryKey: String? = null,
    selectable: Boolean? = null,
) : BasePreference(
    key = key,
    titleKey = titleKey,
    summaryKey = summaryKey,
    selectable = selectable,
    tag = "Preference",
)

internal class IntentPreference(
    key: String,
    titleKey: String,
    summaryKey: String? = null,
    icon: String? = null,
    private val intent: Intent,
) : BasePreference(
    key = key,
    titleKey = titleKey,
    summaryKey = summaryKey,
    icon = icon,
    tag = "Preference",
) {
    override fun serialize(document: Document): Element =
        super.serialize(document).apply {
            appendChild(document.createElement("intent").also { intentElement ->
                intent.action?.let { intentElement.setAttribute("android:action", it) }
                intent.data?.let { intentElement.setAttribute("android:data", it) }
                intent.targetClass?.let { intentElement.setAttribute("android:targetClass", it) }
                intent.targetPackage?.let { intentElement.setAttribute("android:targetPackage", it) }
            })
        }

    data class Intent(
        val action: String? = null,
        val data: String? = null,
        val targetPackage: String? = null,
        val targetClass: String? = null,
    )
}
