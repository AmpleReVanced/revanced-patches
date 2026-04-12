package app.revanced.patches.shared.misc.packagename

import app.morphe.patcher.patch.Option
import app.morphe.patcher.patch.ResourcePatch
import app.morphe.patcher.patch.ResourcePatchContext
import app.morphe.patcher.patch.resourcePatch
import app.morphe.util.asSequence
import app.morphe.util.getNode
import org.w3c.dom.Element
import kotlin.sequences.forEach

/**
 * For DCinside, KakaoTalk only
 */
internal fun resolvePackageName(
    packageNameOption: Option<String>,
    currentPackageName: String,
): String {
    val replacementPackageName = packageNameOption.value

    return if (replacementPackageName != packageNameOption.default) {
        replacementPackageName!!
    } else {
        "$currentPackageName.revanced"
    }
}

/**
 * For DCinside, KakaoTalk only
 */
internal fun baseChangePackageNamePatch(
    packageName: () -> Option<String>,
    updatePermissions: () -> Boolean,
    updateProviders: () -> Boolean,
    extraChanges: ResourcePatchContext.(currentPackageName: String, newPackageName: String) -> Unit = { _, _ -> },
): ResourcePatch = resourcePatch {
    finalize {
        lateinit var currentPackageName: String
        lateinit var newPackageName: String

        document("AndroidManifest.xml").use { document ->
            val manifest = document.getNode("manifest") as Element
            currentPackageName = manifest.getAttribute("package")
            newPackageName = resolvePackageName(packageName(), currentPackageName)

            manifest.setAttribute("package", newPackageName)

            if (updatePermissions()) {
                val permissions = manifest.getElementsByTagName("permission").asSequence()
                val usesPermissions = manifest.getElementsByTagName("uses-permission").asSequence()

                (permissions + usesPermissions)
                    .map { it as Element }
                    .forEach {
                        val name = it.getAttribute("android:name")
                        if (name.startsWith("$currentPackageName.")) {
                            it.setAttribute("android:name", name.replace(currentPackageName, newPackageName))
                        }
                    }
            }

            if (updateProviders()) {
                val providers = manifest.getElementsByTagName("provider").asSequence()

                for (node in providers) {
                    val provider = node as Element

                    val authorities = provider.getAttribute("android:authorities")
                    if (!authorities.startsWith("$currentPackageName.")) continue

                    provider.setAttribute("android:authorities", authorities.replace(currentPackageName, newPackageName))
                }
            }
        }

        extraChanges(currentPackageName, newPackageName)
    }
}