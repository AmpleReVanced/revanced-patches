package app.revanced.patches.dcinside.ad

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.document
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.morphe.util.asSequence
import app.revanced.patches.dcinside.ad.fingerprints.GetMinimumDimensFingerprint
import app.revanced.patches.dcinside.ad.fingerprints.ReadFooterAdContainerSetupFingerprint
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

@Suppress("unused")
internal val dimensBytecodePatch = patch {
    execute {
        GetMinimumDimensFingerprint.method.returnEarly(0)

        ReadFooterAdContainerSetupFingerprint.apply {
            val index = instructionMatches.last().index
            val call = method.getInstruction<FiveRegisterInstruction>(index)
            method.addInstruction(index, "const/4 v${call.registerD}, 0x0")
        }
    }
}

@Suppress("unused")
val dimensPatch = patch(
    name = "Dimens Patch",
    description = "reassigns ad_minimum_height to 0dp to remove ads from the app.",
) {
    compatibleWith(COMPATIBILITY_DC_INSIDE)
    dependsOn(dimensBytecodePatch)

    execute {
        document("res/values/dimens.xml").use { document ->
            val adMinimumHeight = document.getElementsByTagName("dimen")
                .asSequence()
                .filter { it.attributes.getNamedItem("name").nodeValue == "ad_main_small_native" ||
                        it.attributes.getNamedItem("name").nodeValue == "ad_minimum" ||
                        it.attributes.getNamedItem("name").nodeValue == "ad_minimum_tall" ||
                        it.attributes.getNamedItem("name").nodeValue == "main_ad_live_best_spacing" ||
                        it.attributes.getNamedItem("name").nodeValue == "read_ad_minimum" ||
                        it.attributes.getNamedItem("name").nodeValue == "image_ad" }
                .toList()

            if (adMinimumHeight.none()) {
                println("No ad_minimum_height found in dimens.xml, skipping patch.")
                return@use
            }

            adMinimumHeight.forEach { node ->
                node.textContent = "0dp"
            }
        }
    }
}