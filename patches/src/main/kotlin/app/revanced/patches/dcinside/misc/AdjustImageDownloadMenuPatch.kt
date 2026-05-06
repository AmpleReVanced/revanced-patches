package app.revanced.patches.dcinside.misc

import app.morphe.patcher.patch.intOption
import app.morphe.patcher.patch.resourcePatch
import app.morphe.util.asSequence
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE
import org.w3c.dom.Element

private const val IMAGE_BLOCK_LAYOUT = "res/layout/view_setting_image_block.xml"
private const val IMAGE_BLOCK_WRAP_ID = "setting_image_block_wrap"

private fun Element.hasId(id: String) =
    getAttribute("android:id") == "@id/$id" ||
        getAttribute("android:id") == "@+id/$id"

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
        description = "Bottom padding in dp for the image download menu. The original value is 10dp.",
        default = 180,
        required = true,
    ) { value -> value!! >= 0 }

    execute {
        document(IMAGE_BLOCK_LAYOUT).use { document ->
            val imageBlockWrap = document
                .getElementsByTagName("androidx.constraintlayout.widget.ConstraintLayout")
                .asSequence()
                .mapNotNull { it as? Element }
                .firstOrNull { it.hasId(IMAGE_BLOCK_WRAP_ID) }
                ?: error("Could not find $IMAGE_BLOCK_WRAP_ID in $IMAGE_BLOCK_LAYOUT")

            imageBlockWrap.setAttribute("android:paddingBottom", "${bottomPadding}dp")
        }
    }
}
