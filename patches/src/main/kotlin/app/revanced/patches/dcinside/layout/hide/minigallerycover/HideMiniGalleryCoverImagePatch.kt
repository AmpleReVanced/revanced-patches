package app.revanced.patches.dcinside.layout.hide.minigallerycover

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.classDefBy
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import app.morphe.patcher.patch.patch
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.dcinside.misc.settings.PreferenceScreen
import app.revanced.patches.dcinside.misc.settings.addSettingsPatch
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.util.smaliReference
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val SETTINGS_CLASS = "Lapp/revanced/extension/dcinside/settings/Settings;"
private const val EXTENSION_CLASS =
    "Lapp/revanced/extension/dcinside/patches/HideMiniGalleryCoverImagePatch;"

@Suppress("unused")
val hideMiniGalleryCoverImagePatch = patch(
    name = "Hide mini gallery cover image",
    description = "Adds a setting to hide the mini gallery cover image above post lists.",
) {
    compatibleWith(COMPATIBILITY_DC_INSIDE)
    dependsOn(addSettingsPatch)

    execute {
        PreferenceScreen.FEATURES.addPreferences(
            SwitchPreference(
                key = "morphe_pref_hide_mini_gallery_cover_image",
                titleKey = "morphe_settings_hide_mini_gallery_cover_image",
                summary = true,
            ),
        )
        setExtensionIsPatchIncluded(EXTENSION_CLASS)

        val method = MiniGalleryHeaderSetupFingerprint.method
        val miniGalleryTypeCheck = method.findMiniGalleryTypeCheck()
            ?: throw PatchException("Could not find mini gallery type check")
        val hideHeaderMethod = MiniGalleryHeaderSetupFingerprint.classDef
            .findMiniGalleryHeaderHideMethod(method.definingClass)
            ?: throw PatchException("Could not infer mini gallery header hide method")

        val enumType = miniGalleryTypeCheck.galleryInfoMethod.returnType
        val constants = classDefBy(enumType).methods.single { it.name == "<clinit>" }.instructions
        val miniIndex = constants.indexOfFirst { it.getReference<StringReference>()?.string == "MINI" }
        check(miniIndex >= 0) { "Could not find the MINI gallery type" }
        val mini = constants.drop(miniIndex).firstNotNullOf {
            it.getReference<FieldReference>()?.takeIf { field ->
                it.opcode == Opcode.SPUT_OBJECT && field.type == enumType
            }
        }
        val result = method.getInstruction<OneRegisterInstruction>(miniGalleryTypeCheck.moveResultIndex).registerA
        val receiver = method.implementation!!.registerCount - 2
        val registers = method.getFreeRegisterProvider(miniGalleryTypeCheck.moveResultIndex + 1, 2, result, receiver, receiver + 1)
        val setting = registers.getFreeRegister4Bit()
        val miniRegister = registers.getFreeRegister4Bit()
        method.addInstructionsWithLabels(
            miniGalleryTypeCheck.moveResultIndex + 1,
            """
                if-eqz v$result, :morphe_hide_mini_gallery_cover_image_continue
                invoke-static {}, $SETTINGS_CLASS->hideMiniGalleryCoverImage()Z
                move-result v$setting
                if-eqz v$setting, :morphe_hide_mini_gallery_cover_image_continue
                invoke-virtual/range {p1 .. p1}, ${miniGalleryTypeCheck.galleryInfoMethod.smaliReference}
                move-result-object v$setting
                sget-object v$miniRegister, $mini
                if-ne v$setting, v$miniRegister, :morphe_hide_mini_gallery_cover_image_continue
                invoke-virtual/range {p0 .. p0}, ${hideHeaderMethod.noArgVoidMethodReference}
                return-void
                :morphe_hide_mini_gallery_cover_image_continue
                nop
            """.trimIndent(),
        )

        val restoreMethod = MiniGalleryHeaderRestoreFingerprint.method
        val galleryInfoField = restoreMethod.findGalleryInfoFieldReference()
            ?: throw PatchException("Could not find gallery info field")

        restoreMethod.addInstructionsWithLabels(
            0,
            """
                invoke-static {}, $SETTINGS_CLASS->hideMiniGalleryCoverImage()Z
                move-result v0
                if-eqz v0, :morphe_hide_mini_gallery_cover_image_restore_continue
                iget-object v0, p0, ${galleryInfoField.smaliReference}
                if-eqz v0, :morphe_hide_mini_gallery_cover_image_restore_continue
                invoke-virtual {v0}, ${miniGalleryTypeCheck.galleryInfoMethod.smaliReference}
                move-result-object v0
                sget-object v1, $mini
                if-ne v0, v1, :morphe_hide_mini_gallery_cover_image_restore_continue
                invoke-virtual/range {p0 .. p0}, ${hideHeaderMethod.noArgVoidMethodReference}
                return-void
                :morphe_hide_mini_gallery_cover_image_restore_continue
                nop
            """.trimIndent(),
        )
    }
}

private val Method.noArgVoidMethodReference: String
    get() = "$definingClass->$name()V"
