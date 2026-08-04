package app.revanced.patches.kakaotalk.media

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.util.findFreeRegister
import app.morphe.util.getReference
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.kakaotalk.media.fingerprints.ChatMediaSenderMediaItemBundleFingerprint
import app.revanced.patches.kakaotalk.media.fingerprints.ChatMediaSenderUriBundleFingerprint
import app.revanced.patches.kakaotalk.media.fingerprints.OpenChatWebpBundleGuardFingerprint
import app.revanced.patches.kakaotalk.settings.PreferenceScreen
import app.revanced.patches.kakaotalk.settings.addSettingsTabPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val EXTENSION_CLASS =
    "Lapp/revanced/extension/kakaotalk/patches/AllowOpenChatMediaBundlePatch;"

private const val ALLOW_OPEN_CHAT_MEDIA_BUNDLE =
    "Lapp/revanced/extension/kakaotalk/settings/Settings;->allowOpenChatMediaBundle()Z"

@Suppress("unused")
val allowOpenChatMediaBundlePatch = bytecodePatch(
    name = "Allow open chat media bundle",
    description = "Removes the open chat restriction that stops webp, gif and other media from " +
            "being bundled into a single multi-photo message.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(addSettingsTabPatch)

    execute {
        PreferenceScreen.CHAT.addPreferences(
            SwitchPreference(
                key = "morphe_pref_allow_open_chat_media_bundle",
                titleKey = "morphe_settings_patch_allow_open_chat_media_bundle",
                summary = true,
            ),
        )
        setExtensionIsPatchIncluded(EXTENSION_CLASS)

        OpenChatWebpBundleGuardFingerprint.method.apply {
            val free = findFreeRegister(0)

            addInstructionsWithLabels(
                0,
                """
                    invoke-static {}, $ALLOW_OPEN_CHAT_MEDIA_BUNDLE
                    move-result v$free
                    if-eqz v$free, :morphe_original
                    const/4 v$free, 0x0
                    return v$free
                    :morphe_original
                    nop
                """,
            )
        }

        ChatMediaSenderMediaItemBundleFingerprint.method.bundleAllFormatsInOpenMulti()
        ChatMediaSenderUriBundleFingerprint.method.bundleAllFormatsInOpenMulti()
    }
}

private fun MutableMethod.bundleAllFormatsInOpenMulti() {
    val openMultiIndex = instructions.withIndex().first { (_, instruction) ->
        instruction.opcode == Opcode.SGET_OBJECT &&
            instruction.getReference<FieldReference>()?.name == "OpenMulti"
    }.index

    // The send mode is compared against the `OpenMulti` constant immediately after it is read.
    val modeCheckIndex = generateSequence(openMultiIndex + 1) { it + 1 }
        .first { getInstruction(it).opcode == Opcode.IF_NE || getInstruction(it).opcode == Opcode.IF_EQ }

    val insertIndex = modeCheckIndex + 1
    val free = findFreeRegister(insertIndex)

    addInstructionsWithLabels(
        insertIndex,
        """
            invoke-static {}, $ALLOW_OPEN_CHAT_MEDIA_BUNDLE
            move-result v$free
            if-eqz v$free, :morphe_original
            const/4 v$free, 0x1
            return v$free
            :morphe_original
            nop
        """,
    )
}