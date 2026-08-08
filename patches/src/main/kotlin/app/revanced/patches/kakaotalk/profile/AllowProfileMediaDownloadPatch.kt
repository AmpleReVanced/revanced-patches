package app.revanced.patches.kakaotalk.profile

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.all.misc.resources.ResourceType
import app.morphe.patches.all.misc.resources.getResourceId
import app.morphe.patches.all.misc.resources.resourceMappingPatch
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.util.findInstructionIndicesReversedOrThrow
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.kakaotalk.settings.PreferenceScreen
import app.revanced.patches.kakaotalk.settings.addSettingsTabPatch
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS =
    "Lapp/revanced/extension/kakaotalk/patches/AllowProfileMediaDownloadPatch;"

@Suppress("unused")
val allowProfileMediaDownloadPatch = bytecodePatch(
    name = "Allow profile media download",
    description = "Adds the save option of the profile media viewer to the profile pictures and " +
            "background images of other people.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)
    dependsOn(addSettingsTabPatch, resourceMappingPatch)

    execute {
        PreferenceScreen.FEATURES.addPreferences(
            SwitchPreference(
                key = "morphe_pref_allow_profile_media_download",
                titleKey = "morphe_settings_patch_allow_profile_media_download",
                summary = true,
            ),
        )
        setExtensionIsPatchIncluded(EXTENSION_CLASS)

        val screen = ProfileMediaViewerScreenFingerprint

        val iconButtonReference = screen.method.run {
            val iconButtonIndex = indexOfFirstInstructionOrThrow(screen.instructionMatches.first().index) {
                (opcode == Opcode.INVOKE_STATIC || opcode == Opcode.INVOKE_STATIC_RANGE) &&
                        getReference<MethodReference>()?.let {
                            it.returnType == "V" && it.parameterTypes.firstOrNull() == "I"
                        } == true
            }
            getInstruction(iconButtonIndex).getReference<MethodReference>()!!
        }

        val editButtonLabelId = getResourceId(ResourceType.STRING, PROFILE_EDIT_BUTTON_LABEL)

        // Returning before the composer is touched leaves the composition of the action bar intact,
        // unlike branching the edit button out of it.
        mutableClassDefBy(iconButtonReference.definingClass).methods
            .first { it.name == iconButtonReference.name && it.parameterTypes == iconButtonReference.parameterTypes }
            .apply {
                if (!AccessFlags.STATIC.isSet(accessFlags)) {
                    throw PatchException("Profile edit icon button is not static: $iconButtonReference")
                }

                val freeRegisters = getFreeRegisterProvider(0, 2)
                val labelRegister = freeRegisters.getFreeRegister4Bit()
                val editButtonRegister = freeRegisters.getFreeRegister4Bit()

                addInstructionsWithLabels(
                    0,
                    """
                        move/from16 v$labelRegister, p0
                        const v$editButtonRegister, $editButtonLabelId
                        if-ne v$labelRegister, v$editButtonRegister, :morphe_original
                        invoke-static {}, $EXTENSION_CLASS->hideProfileEditButton()Z
                        move-result v$labelRegister
                        if-eqz v$labelRegister, :morphe_original
                        return-void
                        :morphe_original
                        nop
                    """
                )
            }

        // Saving is already implemented, but the action bar only renders for your own profiles.
        // Widening it here, rather than in the check it calls, keeps the original answer available
        // to the edit button of that same action bar within this very composition.
        screen.method.addInstructions(
            0,
            """
                invoke-static/range { p1 .. p1 }, $EXTENSION_CLASS->onProfileActionBar(Z)Z
                move-result p1
            """
        )

        // The mini profile image viewer is a separate screen without such an action bar, and its
        // onCreate returns separately for video and for image profiles.
        ProfileItemDetailOnCreateFingerprint.method.apply {
            findInstructionIndicesReversedOrThrow(Opcode.RETURN_VOID).forEach { returnIndex ->
                addInstruction(
                    returnIndex,
                    "invoke-static { p0 }, $EXTENSION_CLASS->setUpProfileItemDetailDownload(Landroid/app/Activity;)V"
                )
            }
        }
    }
}