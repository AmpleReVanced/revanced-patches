package app.revanced.patches.kakaotalk.misc.settings

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.util.getFreeRegisterProvider
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val SETTINGS_ITEM = "Lapp/revanced/extension/kakaotalk/settings/AdaptiveMorpheSettingsItem;"

internal fun addAdaptiveSettingsItem() {
    val routeConstructor = ExternalSettingRouteFingerprint.constructor
    val targetType = routeConstructor.parameterTypes.first().toString()
    mutableClassDefBy(SETTINGS_ITEM).interfaces.addAll(
        listOf(AdaptiveSettingsListFingerprint.itemType, targetType),
    )

    AdaptiveSettingsListFingerprint.apply {
        val result = instructionMatches.last()
        val register = result.getInstruction<OneRegisterInstruction>().registerA
        method.addInstructions(
            result.index + 1,
            """
                invoke-static/range {v$register .. v$register}, $SETTINGS_ITEM->appendTo(Ljava/util/List;)Ljava/util/List;
                move-result-object v$register
            """,
        )
    }

    AdaptiveSettingsRouteFingerprint.method.apply {
        val registers = getFreeRegisterProvider(0, 2)
        val route = registers.getFreeRegister4Bit()
        val tracker = registers.getFreeRegister4Bit()
        addInstructionsWithLabels(
            0,
            """
                instance-of v$route, p1, $SETTINGS_ITEM
                if-eqz v$route, :original
                check-cast p1, $SETTINGS_ITEM
                new-instance v$route, ${routeConstructor.definingClass}
                const/4 v$tracker, 0x0
                invoke-direct {v$route, p1, v$tracker}, $routeConstructor
                return-object v$route
                :original
                nop
            """,
        )
    }

    AdaptiveSettingsOpenHandlerFingerprint.apply {
        val activity = instructionMatches.single().fieldReference
        val register = method.getFreeRegisterProvider(0, 1).getFreeRegister4Bit()
        method.addInstructionsWithLabels(
            0,
            """
                instance-of v$register, p1, $SETTINGS_ITEM
                if-eqz v$register, :original
                iget-object v$register, p0, $activity
                invoke-static {v$register}, $SETTINGS_ITEM->open(Landroid/content/Context;)V
                return-void
                :original
                nop
            """,
        )
    }
}