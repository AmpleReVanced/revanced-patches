package app.revanced.patches.dcinside.packagename

import app.morphe.patcher.patch.resourcePatch
import app.morphe.util.getNode
import app.revanced.patches.all.misc.packagename.changePackageNamePatch
import app.revanced.patches.all.misc.packagename.packageNameOption
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE
import org.w3c.dom.Element

@Suppress("unused")
val updateProviderPatch = resourcePatch(
    name = "Update Provider Patch",
    description = "It allows you to install the clone app just like the original.",
    default = false
) {
    compatibleWith(COMPATIBILITY_DC_INSIDE)
    dependsOn(changePackageNamePatch)

    execute {
        val originalPackageName = "com.dcinside.app.android"
        val replacementPackageName = packageNameOption.value
        val newPackageName = if (replacementPackageName != packageNameOption.default) {
            replacementPackageName!!
        } else {
            "$originalPackageName.revanced"
        }

        document("res/values/strings.xml").use { document ->
            val resources = document.getNode("resources") as Element
            val stringNodes = resources.getElementsByTagName("string")

            for (i in 0 until stringNodes.length) {
                val stringElement = stringNodes.item(i) as Element
                val textContent = stringElement.textContent?.trim()

                if (textContent != null && textContent.startsWith(originalPackageName)) {
                    val updatedValue = textContent.replace(originalPackageName, newPackageName)
                    stringElement.textContent = updatedValue
                }
            }
        }

        document("AndroidManifest.xml").use { document ->
            val manifest = document.getNode("manifest") as Element
            val providerNodes = manifest.getElementsByTagName("provider")

            for (i in 0 until providerNodes.length) {
                val provider = providerNodes.item(i) as Element
                val authorities = provider.getAttribute("android:authorities")

                if (authorities.startsWith(originalPackageName)) {
                    provider.setAttribute(
                        "android:authorities",
                        authorities.replace(originalPackageName, newPackageName),
                    )
                }
            }
        }
    }
}
