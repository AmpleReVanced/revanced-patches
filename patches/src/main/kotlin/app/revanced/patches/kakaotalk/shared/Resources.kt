package app.revanced.patches.kakaotalk.shared

import app.morphe.patches.all.misc.resources.AppLocale
import app.morphe.patches.all.misc.resources.addAppResources
import app.morphe.patches.all.misc.resources.setAddResourceLocale

private val localesKakaoTalk = listOf(
    AppLocale("", ""),
    AppLocale("ko-rKR", "ko"),
)

internal fun addKakaoTalkResources() {
    setAddResourceLocale(localesKakaoTalk)
    addAppResources("kakaotalk")
}