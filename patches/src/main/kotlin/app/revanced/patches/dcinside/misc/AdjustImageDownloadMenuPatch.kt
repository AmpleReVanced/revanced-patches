package app.revanced.patches.dcinside.misc

import app.morphe.patcher.patch.intOption
import app.morphe.patcher.patch.resourcePatch
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE

private const val IMAGE_BLOCK_LAYOUT = "res/layout/view_setting_image_block.xml"

@Suppress("unused")
val adjustImageDownloadMenuPatch = resourcePatch(
    name = "Adjust image download menu",
    description = "Moves the image download menu buttons upward by adding bottom padding.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_DC_INSIDE)

    val bottomPadding by intOption(
        key = "bottomPadding",
        title = "Bottom padding",
        description = "Bottom padding in dp for the image download menu.",
        default = 180,
        required = true,
    ) { value -> value!! >= 0 }

    execute {
        document(IMAGE_BLOCK_LAYOUT).use { document ->
            val root = document.documentElement
            check(root.hasAttribute("android:paddingBottom")) {
                "Could not find android:paddingBottom in $IMAGE_BLOCK_LAYOUT"
            }

            root.setAttribute("android:paddingBottom", "$bottomPadding.0dip")
        }
    }
}
