package app.revanced.patches.dcinside.ad

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getMutableMethod
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.returnEarly
import app.revanced.patches.dcinside.ad.fingerprints.AdPlacementUnitsFingerprint
import app.revanced.patches.dcinside.ad.fingerprints.InitializeAdSdksFingerprint
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val disableAdControllerPatch = patch(
    name = "Disable ad controller",
    description = "Disables the ad controller that manages ads in the app.",
) {
    compatibleWith(COMPATIBILITY_DC_INSIDE)

    execute {
        AdPlacementUnitsFingerprint.instructionMatches.last().instruction
            .getReference<MethodReference>()!!
            .getMutableMethod()
            .addInstructions(
                0,
                """
                    invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;
                    move-result-object v0
                    return-object v0
                """,
            )

        InitializeAdSdksFingerprint.method.returnEarly()
    }
}
