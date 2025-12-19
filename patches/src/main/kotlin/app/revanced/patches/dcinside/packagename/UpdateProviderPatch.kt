package app.revanced.patches.dcinside.packagename

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.all.misc.packagename.changePackageNamePatch
import app.revanced.patches.all.misc.packagename.packageNameOption
import app.revanced.util.getNode

@Suppress("unused")
val updateProviderPatch = resourcePatch(
    name = "Update Provider Patch",
    description = "It allows you to install the clone app just like the original.",
    use = false
) {
    compatibleWith("com.dcinside.app.android"("5.2.4"))
    dependsOn(changePackageNamePatch)

    execute {
        val replacementPackageName = packageNameOption.value
        val newPackageName = if (replacementPackageName != packageNameOption.default) {
            replacementPackageName!!
        } else {
            "com.dcinside.app.android.revanced"
        }

        document("res/values/strings.xml").use { document ->
            val resources = document.getNode("resources") as org.w3c.dom.Element
            val stringNodes = resources.getElementsByTagName("string")

            for (i in 0 until stringNodes.length) {
                val stringElement = stringNodes.item(i) as org.w3c.dom.Element
                val textContent = stringElement.textContent?.trim()

                if (textContent != null && textContent.startsWith("com.dcinside.app.android")) {
                    val updatedValue = textContent.replace("com.dcinside.app.android", newPackageName)
                    stringElement.textContent = updatedValue

                    val stringName = stringElement.getAttribute("name")
                }
            }
        }
    }
}