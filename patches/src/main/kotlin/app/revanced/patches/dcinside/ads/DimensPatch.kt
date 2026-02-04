package app.revanced.patches.dcinside.ads

import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.dcinside.ads.fingerprints.getMinimumDimensFingerprint
import app.revanced.patches.dcinside.ads.fingerprints.readFooterAdContainerSetupFingerprint
import app.revanced.util.asSequence
import app.revanced.util.returnEarly
import com.android.tools.smali.dexlib2.Opcode

@Suppress("unused")
internal val dimensBytecodePatch = bytecodePatch {
    execute {
        getMinimumDimensFingerprint.method.returnEarly(0)

        readFooterAdContainerSetupFingerprint.method.apply {
            val ifLez = instructions.indexOfFirst { it.opcode == Opcode.IF_LEZ }
            replaceInstructions(
                ifLez + 2,
                """
                    invoke-static {v2}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
                    move-result-object v0
                """.trimIndent()
            )
        }
    }
}

@Suppress("unused")
val dimensPatch = resourcePatch(
    name = "Dimens Patch",
    description = "reassigns ad_minimum_height to 0dp to remove ads from the app.",
) {
    compatibleWith("com.dcinside.app.android"("5.2.6"))
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