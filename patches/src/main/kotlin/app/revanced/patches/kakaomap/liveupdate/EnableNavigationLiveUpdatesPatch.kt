package app.revanced.patches.kakaomap.liveupdate

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.util.getReference
import app.morphe.util.getNode
import app.revanced.patches.kakaomap.misc.extension.addExtensionPatch
import app.revanced.patches.kakaomap.shared.Constants.COMPATIBILITY_KAKAO_MAP
import app.revanced.util.parameterRegister
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import org.w3c.dom.Element

private const val EXTENSION_CLASS =
    "Lapp/revanced/extension/kakaomap/liveupdate/LiveUpdatePatch;"

private const val PROMOTED_NOTIFICATIONS_PERMISSION =
    "android.permission.POST_PROMOTED_NOTIFICATIONS"

private val addLiveUpdatePermissionPatch = resourcePatch {
    compatibleWith(COMPATIBILITY_KAKAO_MAP)

    execute {
        document("AndroidManifest.xml").use { document ->
            val manifest = document.getNode("manifest") as Element
            val permissionExists = manifest.getElementsByTagName("uses-permission").let { permissions ->
                (0 until permissions.length).any { index ->
                    (permissions.item(index) as? Element)
                        ?.getAttribute("android:name") == PROMOTED_NOTIFICATIONS_PERMISSION
                }
            }

            if (!permissionExists) {
                val application = manifest.getElementsByTagName("application").item(0)
                manifest.insertBefore(
                    document.createElement("uses-permission").apply {
                        setAttribute("android:name", PROMOTED_NOTIFICATIONS_PERMISSION)
                    },
                    application,
                )
            }
        }
    }
}

@Suppress("unused")
val enableNavigationLiveUpdatesPatch = bytecodePatch(
    name = "Enable navigation live updates",
    description = "Shows navigation progress with Android Live Updates.",
) {
    compatibleWith(COMPATIBILITY_KAKAO_MAP)
    dependsOn(addExtensionPatch, addLiveUpdatePermissionPatch)

    execute {
        PubtransLocationWarmupFingerprint.apply {
            val thresholdIndex = instructionMatches.single().index
            val thresholdRegister = method
                .getInstruction<OneRegisterInstruction>(thresholdIndex)
                .registerA
            method.replaceInstruction(thresholdIndex, "const/4 v$thresholdRegister, 0x2")
        }

        BusAndWalkLocationMatchFingerprint.apply {
            val updateLocationIndex = method.implementation!!.instructions
                .mapIndexedNotNull { index, instruction ->
                    val reference = instruction.getReference<MethodReference>()
                    if (reference?.name == "updateLocation" &&
                        reference.parameterTypes.map(CharSequence::toString) == listOf(
                            "Lcom/kakao/vectormap/Coordinate;",
                            "D",
                            "Ljava/lang/Double;",
                            "Ljava/lang/Double;",
                        ) &&
                        reference.returnType == "Z"
                    ) index else null
                }
                .singleOrNull()
                ?: throw PatchException("Could not resolve the matched location state")
            val matchedResultIndex = updateLocationIndex + 1
            val matchedRegister = method
                .getInstruction<OneRegisterInstruction>(matchedResultIndex)
                .registerA
            method.addInstructions(
                matchedResultIndex + 1,
                "invoke-static {v$matchedRegister}, $EXTENSION_CLASS->markLocationMatched(Z)V",
            )
        }

        PubtransLocationUpdateFingerprint.method.addInstructions(
            0,
            "invoke-static {}, $EXTENSION_CLASS->markLocationUpdated()V",
        )

        PubtransSectionDistanceRatioFingerprint.method.apply {
            implementation!!.instructions
                .mapIndexedNotNull { index, instruction ->
                    if (instruction.opcode == Opcode.RETURN_WIDE) index else null
                }
                .asReversed()
                .forEach { index ->
                    val ratioRegister = getInstruction<OneRegisterInstruction>(index).registerA
                    addInstructions(
                        index,
                        "invoke-static/range {v$ratioRegister .. v${ratioRegister + 1}}, " +
                            "$EXTENSION_CLASS->captureSectionDistanceRatio(D)V",
                    )
                }
        }

        PubtransJourneyStartFingerprint.method.apply {
            val isEmptyIndex = implementation!!.instructions
                .mapIndexedNotNull { index, instruction ->
                    val reference = instruction.getReference<MethodReference>()
                    if (reference?.definingClass == "Ljava/util/List;" &&
                        reference.name == "isEmpty" &&
                        reference.parameterTypes.isEmpty() &&
                        reference.returnType == "Z"
                    ) index else null
                }
                .singleOrNull()
                ?: throw PatchException("Could not resolve the public transit journey source")
            val listRegister = getInstruction<FiveRegisterInstruction>(isEmptyIndex).registerC
            addInstructions(
                isEmptyIndex,
                "invoke-static {v$listRegister}, $EXTENSION_CLASS->beginJourney(Ljava/util/List;)V",
            )
        }

        PubtransJourneyCompleteFingerprint.method.addInstructions(
            0,
            "invoke-static {}, $EXTENSION_CLASS->completeJourney()V",
        )

        val progressSourceMethod = PubtransJourneyProgressFingerprint.method
        val guideStepListField = progressSourceMethod.implementation!!.instructions
            .mapNotNull { instruction -> instruction.getReference<FieldReference>() }
            .filter { reference ->
                reference.definingClass == progressSourceMethod.definingClass &&
                    reference.type == "Ljava/util/List;"
            }
            .distinctBy { reference ->
                "${reference.definingClass}->${reference.name}:${reference.type}"
            }
            .singleOrNull()
            ?: throw PatchException("Could not resolve the public transit guide step list")

        PubtransNotificationGenerationFingerprint.method.apply {
            val receiverRegister = parameterRegister(0) - 1
            val parentIndexRegister = parameterRegister(2)
            val childIndexRegister = parameterRegister(3)
            val stateRegister = parameterRegister(4)
            addInstructions(
                0,
                """
                    iget-object v0, v$receiverRegister, ${guideStepListField.definingClass}->${guideStepListField.name}:${guideStepListField.type}
                    invoke-static {v0, v$parentIndexRegister, v$childIndexRegister, v$stateRegister}, $EXTENSION_CLASS->captureProgress(Ljava/util/List;III)V
                """.trimIndent(),
            )
        }

        PubtransRemoteViewsSetTextFingerprint.method.apply {
            val viewIdRegister = parameterRegister(0)
            val textRegister = parameterRegister(1)
            addInstructions(
                0,
                "invoke-static/range {v$viewIdRegister .. v$textRegister}, " +
                    "$EXTENSION_CLASS->captureText(ILjava/lang/CharSequence;)V",
            )
        }

        JourneyNotificationBuildFingerprint.matchAll(6..6)
            .forEach { match ->
                val notificationIndex = match.instructionMatches[1].index
                val notificationRegister =
                    match.method.getInstruction<OneRegisterInstruction>(notificationIndex).registerA

                match.method.addInstructions(
                    notificationIndex + 1,
                    """
                        invoke-static/range {v$notificationRegister .. v$notificationRegister}, $EXTENSION_CLASS->promote(Landroid/app/Notification;)Landroid/app/Notification;
                        move-result-object v$notificationRegister
                    """.trimIndent(),
                )
            }
    }
}