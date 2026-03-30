package app.revanced.patches.dcinside.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.all.misc.resources.addResourcesPatch
import app.morphe.util.asSequence
import app.morphe.util.getReference
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import org.w3c.dom.Element

private const val NOTIFICATION_SHORTCUT_ID_NAME = "morphe_dcinside_post_list_quick_notifi"

private val addNotificationShortcutBytecodePatch = bytecodePatch {
    compatibleWith(COMPATIBILITY_DC_INSIDE)
    dependsOn(addExtensionPatch, sharedExtensionPatch, addResourcesPatch)

    execute {
        QuickWriteVisibilityFingerprint.method.apply {
            val setVisibilityIndex = instructions.indexOfFirst {
                it.opcode == Opcode.INVOKE_VIRTUAL &&
                    it.getReference<MethodReference>()?.name == "setVisibility" &&
                    it.getReference<MethodReference>()?.definingClass == "Landroid/view/View;"
            }

            check(setVisibilityIndex >= 0) {
                "Could not find quick write visibility call in ${definingClass}->$name"
            }

            addInstructionsWithLabels(
                setVisibilityIndex + 1,
                """
                    const-string v0, "$NOTIFICATION_SHORTCUT_ID_NAME"
                    const-string v1, "id"
                    invoke-static {v1, v0}, Lapp/revanced/extension/dcinside/helper/ResourceHelper;->getResourceId(Ljava/lang/String;Ljava/lang/String;)I
                    move-result v0
                    invoke-virtual {p0}, Landroidx/fragment/app/Fragment;->requireView()Landroid/view/View;
                    move-result-object v1
                    invoke-virtual {v1, v0}, Landroid/view/View;->findViewById(I)Landroid/view/View;
                    move-result-object v0
                    if-eqz v0, :revanced_notification_shortcut_end
                    invoke-virtual {v0, p1}, Landroid/view/View;->setVisibility(I)V
                    :revanced_notification_shortcut_end
                    const-string v1, "postListQuickWrite"
                """.trimIndent()
            )
        }

        PostListOnViewCreatedFingerprint.method.apply {
            val quickWriteSetupIndex = instructions.indexOfLast {
                it.getReference<MethodReference>()?.name == "S" &&
                    it.getReference<MethodReference>()?.definingClass == "Lcom/dcinside/app/internal/r;"
            }

            check(quickWriteSetupIndex >= 0) {
                "Could not find quick write setup call in ${definingClass}->$name"
            }

            addInstructionsWithLabels(
                quickWriteSetupIndex + 1,
                """
                    const-string v4, "$NOTIFICATION_SHORTCUT_ID_NAME"
                    const-string v5, "id"
                    invoke-static {v5, v4}, Lapp/revanced/extension/dcinside/helper/ResourceHelper;->getResourceId(Ljava/lang/String;Ljava/lang/String;)I
                    move-result v4
                    invoke-virtual {p1, v4}, Landroid/view/View;->findViewById(I)Landroid/view/View;
                    move-result-object v4
                    if-eqz v4, :revanced_notification_click_listener_end
                    new-instance v5, Lapp/revanced/extension/dcinside/NotificationClickListener;
                    invoke-virtual {p0}, Landroidx/fragment/app/Fragment;->requireContext()Landroid/content/Context;
                    move-result-object v6
                    invoke-direct {v5, v6}, Lapp/revanced/extension/dcinside/NotificationClickListener;-><init>(Landroid/content/Context;)V
                    invoke-virtual {v4, v5}, Landroid/view/View;->setOnClickListener(Landroid/view/View${'$'}OnClickListener;)V
                    :revanced_notification_click_listener_end
                    nop
                """.trimIndent()
            )
        }
    }
}

@Suppress("unused")
val addNotificationShortcutPatch = resourcePatch(
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
