package app.revanced.patches.dcinside.layout.shortcut

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.document
import app.morphe.patcher.patch.patch
import app.morphe.patches.all.misc.resources.addResourcesPatch
import app.morphe.util.asSequence
import app.revanced.patches.dcinside.misc.extension.addExtensionPatch
import app.revanced.patches.dcinside.misc.extension.sharedExtensionPatch
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import org.w3c.dom.Element

private const val NOTIFICATION_SHORTCUT_ID_NAME = "morphe_dcinside_post_list_quick_notifi"

private const val NOTIFICATION_SHORTCUT_EXTENSION =
    "Lapp/revanced/extension/dcinside/patches/NotificationShortcutPatch;"

private val addNotificationShortcutBytecodePatch = patch {
    compatibleWith(COMPATIBILITY_DC_INSIDE)
    dependsOn(addExtensionPatch, sharedExtensionPatch, addResourcesPatch)

    execute {
        QuickWriteVisibilityFingerprint.apply {
            val index = instructionMatches.first().index
            val call = method.getInstruction<FiveRegisterInstruction>(index)
            method.replaceInstruction(
                index,
                "invoke-static {v${call.registerC}, v${call.registerD}}, " +
                    "$NOTIFICATION_SHORTCUT_EXTENSION->setQuickWriteVisibility(Landroid/view/View;I)V",
            )
        }
        PostListOnViewCreatedFingerprint.apply {
            method.addInstructions(
                instructionMatches.single().index + 1,
                "invoke-static {p1}, $NOTIFICATION_SHORTCUT_EXTENSION->bind(Landroid/view/View;)V",
            )
        }
    }
}

@Suppress("unused")
val addNotificationShortcutPatch = patch(
    name = "Add notification shortcut",
    description = "Adds a notification shortcut button above quick write in the post list.",
) {
    compatibleWith(COMPATIBILITY_DC_INSIDE)
    dependsOn(addNotificationShortcutBytecodePatch)

    execute {
        document("res/values/ids.xml").use { document ->
            val resources = document.documentElement
            val exists = document.getElementsByTagName("id")
                .asSequence()
                .any { it.attributes?.getNamedItem("name")?.nodeValue == NOTIFICATION_SHORTCUT_ID_NAME }

            if (!exists) {
                resources.appendChild(
                    document.createElement("item").apply {
                        setAttribute("type", "id")
                        setAttribute("name", NOTIFICATION_SHORTCUT_ID_NAME)
                    }
                )
            }
        }

        document("res/layout/fragment_post_list.xml").use { document ->
            val quickWrite = document.getElementsByTagName("androidx.constraintlayout.widget.ConstraintLayout")
                .asSequence()
                .mapNotNull { it as? Element }
                .firstOrNull { it.getAttribute("android:id") == "@id/post_list_quick_write" }
                ?: error("Could not find post_list_quick_write in fragment_post_list.xml")

            val exists = document.getElementsByTagName("androidx.constraintlayout.widget.ConstraintLayout")
                .asSequence()
                .mapNotNull { it as? Element }
                .any { it.getAttribute("android:id") == "@id/$NOTIFICATION_SHORTCUT_ID_NAME" }

            if (exists) return@use

            val notificationShortcut = quickWrite.cloneNode(true) as Element
            notificationShortcut.setAttribute("android:id", "@id/$NOTIFICATION_SHORTCUT_ID_NAME")
            notificationShortcut.setAttribute("android:layout_gravity", "end|bottom")
            notificationShortcut.setAttribute("android:layout_marginBottom", "140.0dp")
            notificationShortcut.setAttribute("android:layout_marginEnd", "15.0dp")

            notificationShortcut.childNodes.asSequence()
                .mapNotNull { it as? Element }
                .firstOrNull { it.tagName == "androidx.appcompat.widget.AppCompatImageView" }
                ?.apply {
                    setAttribute("android:padding", "9.0dp")
                    setAttribute("android:src", "@drawable/ic_side_notification")
                }

            quickWrite.parentNode.insertBefore(notificationShortcut, quickWrite.nextSibling)
        }
    }
}
