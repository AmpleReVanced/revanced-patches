package app.revanced.patches.dcinside.ads

import app.revanced.patcher.patch.resourcePatch
import app.revanced.util.asSequence

@Suppress("unused")
val dimensPatch = resourcePatch(
    name = "Dimens Patch",
    description = "reassigns ad_minimum_height to 0dp to remove ads from the app.",
) {
    compatibleWith("com.dcinside.app.android"("5.1.7"))

    execute {
        document("res/values/dimens.xml").use { document ->
            val adMinimumHeight = document.getElementsByTagName("dimen")
                .asSequence()
                .filter { it.attributes.getNamedItem("name").nodeValue == "ad_minimum_height" ||
                        it.attributes.getNamedItem("name").nodeValue == "read_ad_minimum_height" ||
                        it.attributes.getNamedItem("name").nodeValue == "script_ad_size" ||
                        it.attributes.getNamedItem("name").nodeValue == "image_ad_height" ||
                        it.attributes.getNamedItem("name").nodeValue == "main_ad_live_best_spacing" }
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