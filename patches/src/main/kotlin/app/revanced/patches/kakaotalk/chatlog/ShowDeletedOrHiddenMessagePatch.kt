package app.revanced.patches.kakaotalk.chatlog

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.kakaotalk.misc.addExtensionPatch
import app.revanced.patches.kakaotalk.misc.sharedExtensionPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.patches.kakaotalk.shared.addKakaoTalkResources

@Suppress("unused")
val showDeletedOrHiddenMessagePatch = bytecodePatch(
    name = "Show deleted or hidden messages",
    description = "Allows you to see deleted/hidden messages in chat logs.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(addExtensionPatch, addResourcesPatch, sharedExtensionPatch)

    val deletedColorText by stringOption(
        key = "deletedColor",
        title = "Deleted color",
        description = "32-bit ARGB. Accepts 0xAARRGGBB or signed decimal.",
        default = "0xFFFF4444",
    )

    val hiddenColorText by stringOption(
        key = "hiddenColor",
        title = "Hidden color",
        description = "32-bit ARGB. Accepts 0xAARRGGBB or signed decimal.",
        default = "0xFF999999",
    )

    execute {
        addKakaoTalkResources()

        applyDeletedHiddenColors(deletedColorText!!, hiddenColorText!!)
        hookChatInfoViewExtension()

        val chatLog = addChatLogFlagAccessors()
        hookPendingOverwriteRetryAfterChatLogInsert(chatLog)
        hookDirectOverwriteFeeds(chatLog)
        hookChatInfoViewHolderFlags(chatLog)
        keepDeletedAndHiddenChatLogsVisible()
    }
}
