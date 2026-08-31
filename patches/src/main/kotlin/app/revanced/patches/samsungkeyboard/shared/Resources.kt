package app.revanced.patches.samsungkeyboard.shared

import app.morphe.patches.all.misc.resources.AppLocale
import app.morphe.patches.all.misc.resources.addAppResources
import app.morphe.patches.all.misc.resources.setAddResourceLocale

internal fun addSamsungKeyboardResources() {
    setAddResourceLocale(
        listOf(
            AppLocale("", ""),
            AppLocale("ko-rKR", "ko"),
        ),
    )
    addAppResources("samsungkeyboard")
}