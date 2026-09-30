package app.revanced.patches.navermap.liveupdate

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.util.getNode
import app.revanced.patches.navermap.shared.Constants.COMPATIBILITY_NAVER_MAP
import app.revanced.util.parameterRegister
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import org.w3c.dom.Element

private const val EXTENSION_CLASS = "Lapp/revanced/extension/navermap/liveupdate/LiveUpdatePatch;"
private const val PROMOTED_PERMISSION = "android.permission.POST_PROMOTED_NOTIFICATIONS"

private val liveUpdatePermissionPatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { document ->
            val manifest = document.getNode("manifest") as Element
            val permissions = manifest.getElementsByTagName("uses-permission")
            if ((0 until permissions.length).none {
                    (permissions.item(it) as Element).getAttribute("android:name") == PROMOTED_PERMISSION
                }
            ) {
                manifest.insertBefore(
                    document.createElement("uses-permission").apply {
                        setAttribute("android:name", PROMOTED_PERMISSION)
                    },
                    manifest.getElementsByTagName("application").item(0),
                )
            }
        }
    }
}

@Suppress("unused")
val enableNavigationLiveUpdatesPatch = bytecodePatch(
    name = "Enable navigation live updates",
    description = "Converts Now Bar navigation notifications to Android Live Updates on Android 16 or newer.",
) {
    compatibleWith(COMPATIBILITY_NAVER_MAP)
    dependsOn(liveUpdatePermissionPatch)
    extendWith("extensions/navermap.mpe")

    execute {
        OngoingActivityExtrasFingerprint.method.apply {
            val contextRegister = parameterRegister(1)
            addInstructions(
                0,
                "invoke-static/range {v$contextRegister .. v$contextRegister}, " +
                    "$EXTENSION_CLASS->initialize(Landroid/content/Context;)V",
            )
        }

        NavigationOngoingActivitySupportFingerprint(OngoingActivitySupportFingerprint.method)
            .matchAll(4..4).forEach { match ->
                val index = match.instructionMatches[1].index
                val register = match.method.getInstruction<OneRegisterInstruction>(index).registerA
                match.method.addInstructions(
                    index + 1,
                    """
                        invoke-static/range {v$register .. v$register}, $EXTENSION_CLASS->supportsLiveUpdates(Z)Z
                        move-result v$register
                    """.trimIndent(),
                )
            }

        NotificationBuildFingerprint(
            OngoingActivityExtrasFingerprint.method.parameterTypes[0].toString(),
        ).method.apply {
            implementation!!.instructions.mapIndexedNotNull { index, instruction ->
                if (instruction.opcode == Opcode.RETURN_OBJECT) index else null
            }.asReversed().forEach { index ->
                val register = getInstruction<OneRegisterInstruction>(index).registerA
                addInstructions(
                    index,
                    """
                        invoke-static/range {v$register .. v$register}, $EXTENSION_CLASS->promote(Landroid/app/Notification;)Landroid/app/Notification;
                        move-result-object v$register
                    """.trimIndent(),
                )
            }
        }
    }
}