package app.revanced.patches.kakaotalk.settings.preference

import java.io.Closeable

internal abstract class BasePreferenceScreen : Closeable {
    private val root = linkedSetOf<Screen>()
    private var committed = false

    override fun close() {
        if (committed) return
        committed = true
        root.forEach { commit(it.transform()) }
    }

    protected abstract fun commit(preference: BasePreference)

    inner class Screen(
        private val key: String,
        private val titleKey: String,
        private val categories: MutableSet<Category> = linkedSetOf(),
        private val preferences: MutableSet<BasePreference> = linkedSetOf(),
    ) {
        fun addPreferences(vararg preferences: BasePreference) {
            root += this
            this.preferences += preferences
        }

        fun category(
            key: String,
            titleKey: String,
        ) = Category(key, titleKey).also { category ->
            root += this
            categories += category
        }

        fun transform(): BasePreference =
            PreferenceCategory(
                key = key,
                titleKey = titleKey,
                preferences = preferences + categories.map { it.transform() },
            )
    }

    inner class Category(
        private val key: String,
        private val titleKey: String,
        private val preferences: MutableSet<BasePreference> = linkedSetOf(),
    ) {
        fun addPreferences(vararg preferences: BasePreference) {
            this.preferences += preferences
        }

        fun transform(): PreferenceCategory =
            PreferenceCategory(
                key = key,
                titleKey = titleKey,
                preferences = preferences,
            )
    }
}