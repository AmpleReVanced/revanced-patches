package app.revanced.patches.dcinside.packagename

import app.morphe.patcher.patch.Option
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.util.getNode
import app.revanced.patches.dcinside.integrity.bypassGetTextPath
import app.revanced.patches.dcinside.integrity.spoofSignaturePatch
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE
import app.revanced.patches.shared.misc.packagename.baseChangePackageNamePatch
import org.w3c.dom.Element

lateinit var packageNameOption: Option<String>

@Suppress("unused")
val changePackageNamePatch = bytecodePatch(
    name = "Change package name",
    description = "Appends \".revanced\" to the package name by default. " +
            "For DCinside only",
    default = false,
) {
    packageNameOption = stringOption(
        key = "packageName",
        default = "Default",
        values = mapOf("Default" to "Default"),
        title = "Package name",
        description = "The name of the package to rename the app to.",
        required = true,
    ) {
        it == "Default" || it!!.matches(Regex("^[a-z]\\w*(\\.[a-z]\\w*)+\$"))
    }

    val updatePermissions by booleanOption(
        key = "updatePermissions",
        default = false,
        title = "Update permissions",
        description = "Update compatibility receiver permissions. " +
                "Enabling this can fix installation errors, but this can also break features in certain apps.",
    )

    val updateProviders by booleanOption(
        key = "updateProviders",
        default = false,
        title = "Update providers",
        description = "Update provider names declared by the app. " +
                "Enabling this can fix installation errors, but this can also break features in certain apps.",
    )

    compatibleWith(COMPATIBILITY_DC_INSIDE)
    dependsOn(
        baseChangePackageNamePatch(
            packageName = { packageNameOption },
            updatePermissions = { updatePermissions == true },
            updateProviders = { updateProviders == true },
            extraChanges = { currentPackageName, newPackageName ->
                if (updateProviders != true) return@baseChangePackageNamePatch

                document("res/values/strings.xml").use { document ->
                    val resources = document.getNode("resources") as Element
                    val stringNodes = resources.getElementsByTagName("string")

                    for (index in 0 until stringNodes.length) {
                        val stringElement = stringNodes.item(index) as Element
                        val textContent = stringElement.textContent?.trim() ?: continue
                        if (!textContent.startsWith(currentPackageName)) continue

                        stringElement.textContent = textContent.replace(currentPackageName, newPackageName)
                    }
                }
            },
        ),
        bypassGetTextPath,
        spoofSignaturePatch,
    )
}
