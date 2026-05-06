package app.revanced.patches.dcinside.shared

import app.morphe.patches.all.misc.resources.AppLocale
import app.morphe.patches.all.misc.resources.addAppResources
import app.morphe.patches.all.misc.resources.setAddResourceLocale

private val localesDcInside = listOf(
    AppLocale("", ""),
    AppLocale("ko-rKR", "ko"),
)

internal fun addDcInsideResources() {
    setAddResourceLocale(localesDcInside)
    addAppResources("dcinside")
}
