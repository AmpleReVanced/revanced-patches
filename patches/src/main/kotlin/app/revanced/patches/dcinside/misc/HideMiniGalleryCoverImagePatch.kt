package app.revanced.patches.dcinside.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.revanced.patches.dcinside.settings.addSettingsPatch
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE
import app.revanced.util.smaliReference
import com.android.tools.smali.dexlib2.iface.Method

private const val SETTINGS_CLASS = "Lapp/revanced/extension/dcinside/settings/Settings;"

@Suppress("unused")
val hideMiniGalleryCoverImagePatch = bytecodePatch(
    name = "Hide mini gallery cover image",
    description = "Adds a setting to hide the mini gallery cover image above post lists.",
) {
    compatibleWith(COMPATIBILITY_DC_INSIDE)
    dependsOn(addSettingsPatch)

    execute {
        val method = MiniGalleryHeaderSetupFingerprint.method
        val miniGalleryTypeCheck = method.findMiniGalleryTypeCheck()
            ?: throw PatchException("Could not find mini gallery type check")
        val hideHeaderMethod = MiniGalleryHeaderSetupFingerprint.classDef
            .findMiniGalleryHeaderHideMethod(method.definingClass)
            ?: throw PatchException("Could not infer mini gallery header hide method")

        method.addInstructionsWithLabels(
            miniGalleryTypeCheck.moveResultIndex + 1,
            """
                if-eqz v1, :morphe_hide_mini_gallery_cover_image_continue
                invoke-static {}, $SETTINGS_CLASS->hideMiniGalleryCoverImage()Z
                move-result v2
                if-eqz v2, :morphe_hide_mini_gallery_cover_image_continue
                invoke-direct {p0}, ${hideHeaderMethod.noArgVoidMethodReference}
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
                invoke-virtual {v0}, ${miniGalleryTypeCheck.galleryTypeCheckMethod.smaliReference}
                move-result v0
                if-eqz v0, :morphe_hide_mini_gallery_cover_image_restore_continue
                invoke-direct {p0}, ${hideHeaderMethod.noArgVoidMethodReference}
                return-void
                :morphe_hide_mini_gallery_cover_image_restore_continue
                nop
            """.trimIndent(),
        )
    }
}

private val Method.noArgVoidMethodReference: String
    get() = "$definingClass->$name()V"
