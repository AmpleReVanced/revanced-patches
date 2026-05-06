package app.revanced.patches.dcinside.branding

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.ResourcePatchContext
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.all.misc.resources.resourceMappingPatch
import app.morphe.util.ResourceGroup
import app.morphe.util.copyResources
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE
import app.revanced.util.removeStringsElements
import java.io.File
import java.util.logging.Logger
import org.w3c.dom.Element

private val mipmapDirectories = mapOf(
    "mipmap-mdpi" to "48x48 px",
    "mipmap-hdpi" to "72x72 px",
    "mipmap-xhdpi" to "96x96 px",
    "mipmap-xxhdpi" to "144x144 px",
    "mipmap-xxxhdpi" to "192x192 px",
)

private val notificationIconDirectories = mapOf(
    "drawable-mdpi" to "24x24 px",
    "drawable-hdpi" to "36x36 px",
    "drawable-xhdpi" to "48x48 px",
    "drawable-xxhdpi" to "72x72 px",
    "drawable-xxxhdpi" to "96x96 px",
)

private val launcherIconFileNames = arrayOf("ic_launcher.png", "ic_launcher.webp")
private val notificationRasterIconFileNames = arrayOf("ic_symbol.png", "ic_symbol.webp")

private const val notificationVectorIconFileName = "ic_symbol.xml"
private const val bundledCustomBrandingResourceDirectory = "custom-branding"
private const val originalIconOptionValue = "Original"
private const val bundledIconOptionValue = "Bundled"
private const val customIconStyleName = "custom"
private const val launcherResourceNamePrefix = "morphe_launcher_"
private const val launcherAdaptiveBackgroundPrefix = "morphe_adaptive_background_"
private const val launcherAdaptiveForegroundPrefix = "morphe_adaptive_foreground_"
private const val launcherAdaptiveMonochromePrefix = "morphe_adaptive_monochrome_"
private const val notificationIconNamePrefix = "morphe_notification_icon_"
private const val bundledLauncherIconResourceName = "@mipmap/morphe_launcher_custom"
private const val bundledNotificationIconResourceName = "@drawable/morphe_notification_icon_custom"
private const val loggerName = "app.revanced.patches.dcinside.branding.CustomBrandingPatch"

private fun File.findOptionalFile(vararg names: String): File? {
    val matches = names.map(::resolve).filter(File::isFile)
    if (matches.size > 1) {
        throw PatchException("Only one of ${names.joinToString()} can exist in $absolutePath")
    }

    return matches.singleOrNull()
}

private fun File.deleteResourcesByBaseName(baseName: String) {
    listFiles()
        ?.filter { it.isFile && it.nameWithoutExtension == baseName }
        ?.forEach { file ->
            if (!file.delete()) {
                throw PatchException("Failed to delete ${file.absolutePath}")
            }
        }
}

private fun ResourcePatchContext.copyCustomIcons(customIconPath: String) {
    val iconPathFile = File(customIconPath.trim())
    if (!iconPathFile.exists()) {
        throw PatchException("The custom icon path cannot be found: ${iconPathFile.absolutePath}")
    }
    if (!iconPathFile.isDirectory) {
        throw PatchException("The custom icon path must be a folder: ${iconPathFile.absolutePath}")
    }

    val resourceDirectory = get("res")
    var copiedLauncherIcon = false
    var copiedNotificationIcon = false

    mipmapDirectories.keys.forEach { directoryName ->
        val sourceDirectory = iconPathFile.resolve(directoryName)
        if (!sourceDirectory.isDirectory) return@forEach

        val sourceIcon = sourceDirectory.findOptionalFile(*launcherIconFileNames)
            ?: return@forEach

        val targetDirectory = resourceDirectory.resolve(directoryName)
        if (!targetDirectory.exists()) {
            throw PatchException("Resource directory not found: ${targetDirectory.absolutePath}")
        }

        targetDirectory.deleteResourcesByBaseName("ic_launcher")
        sourceIcon.copyTo(
            target = targetDirectory.resolve(sourceIcon.name),
            overwrite = true,
        )
        copiedLauncherIcon = true
    }

    if (!copiedLauncherIcon) {
        throw PatchException(
            "Expected to find at least one launcher icon file (${launcherIconFileNames.joinToString()}) " +
                "inside ${mipmapDirectories.keys.joinToString()} under ${iconPathFile.absolutePath}",
        )
    }

    // Remove the original notification icon before copying any replacement so qualifier matching
    // does not keep selecting the bundled asset.
    resourceDirectory.listFiles()
        ?.filter { it.isDirectory && it.name.startsWith("drawable") }
        ?.forEach { it.deleteResourcesByBaseName("ic_symbol") }

    val drawableDirectory = iconPathFile.resolve("drawable")
    if (drawableDirectory.isDirectory) {
        val vectorIcon = drawableDirectory.resolve(notificationVectorIconFileName)
        if (vectorIcon.isFile) {
            val targetDirectory = resourceDirectory.resolve("drawable").apply { mkdirs() }
            vectorIcon.copyTo(
                target = targetDirectory.resolve(notificationVectorIconFileName),
                overwrite = true,
            )
            copiedNotificationIcon = true
        }
    }

    notificationIconDirectories.keys.forEach { directoryName ->
        val sourceDirectory = iconPathFile.resolve(directoryName)
        if (!sourceDirectory.isDirectory) return@forEach

        val sourceIcon = sourceDirectory.findOptionalFile(*notificationRasterIconFileNames)
            ?: return@forEach

        val targetDirectory = resourceDirectory.resolve(directoryName).apply { mkdirs() }
        targetDirectory.deleteResourcesByBaseName("ic_symbol")
        sourceIcon.copyTo(
            target = targetDirectory.resolve(sourceIcon.name),
            overwrite = true,
        )
        copiedNotificationIcon = true
    }

    if (!copiedNotificationIcon) {
        Logger.getLogger(loggerName).warning(
            "No custom notification icon found. Keeping the notification icon reference as @drawable/ic_symbol.",
        )
    }
}

private fun ResourcePatchContext.copyBundledCustomBrandingIcons() {
    copyResources(
        bundledCustomBrandingResourceDirectory,
        ResourceGroup(
            "drawable",
            "$launcherAdaptiveMonochromePrefix$customIconStyleName.xml",
            "$notificationIconNamePrefix$customIconStyleName.xml",
        ),
        ResourceGroup(
            "mipmap-anydpi",
            "$launcherResourceNamePrefix$customIconStyleName.xml",
        ),
    )

    mipmapDirectories.keys.forEach { dpi ->
        copyResources(
            bundledCustomBrandingResourceDirectory,
            ResourceGroup(
                dpi,
                "$launcherAdaptiveBackgroundPrefix$customIconStyleName.png",
                "$launcherAdaptiveForegroundPrefix$customIconStyleName.png",
            ),
        )
    }
}

private fun ResourcePatchContext.useBundledCustomBrandingIconsInManifest() {
    document("AndroidManifest.xml").use { document ->
        val application = document.getElementsByTagName("application").item(0) as Element
        application.setAttribute("android:icon", bundledLauncherIconResourceName)

        val metaDataNodes = application.getElementsByTagName("meta-data")
        for (index in 0 until metaDataNodes.length) {
            val metaData = metaDataNodes.item(index) as? Element ?: continue
            if (metaData.getAttribute("android:name") == "com.google.firebase.messaging.default_notification_icon") {
                metaData.setAttribute("android:resource", bundledNotificationIconResourceName)
            }
        }

        val activityAliasNodes = application.getElementsByTagName("activity-alias")
        for (index in 0 until activityAliasNodes.length) {
            val activityAlias = activityAliasNodes.item(index) as? Element ?: continue
            if (activityAlias.getAttribute("android:name") == "com.dcinside.app.MainActivity") {
                activityAlias.setAttribute("android:icon", bundledLauncherIconResourceName)
            }
        }
    }
}

@Suppress("unused")
val customBrandingPatch = resourcePatch(
    name = "Custom branding",
    description = "Customize app branding (name and icon)",
    default = false
) {
    compatibleWith(COMPATIBILITY_DC_INSIDE)
    dependsOn(resourceMappingPatch)

    val customName by stringOption(
        key = "customName",
        title = "Custom app name",
        description = "Custom app name",
    )

    val customIcon by stringOption(
        key = "customIcon",
        default = originalIconOptionValue,
        values = mapOf(
            originalIconOptionValue to originalIconOptionValue,
            bundledIconOptionValue to bundledIconOptionValue,
        ),
        title = "Custom app icon",
        description = """
            App icon source.

            Special values:
            - $originalIconOptionValue: Keep the original app icon.
            - $bundledIconOptionValue: Use the bundled icon from patches/src/main/resources/custom-branding.

            Any other value is treated as a folder path with custom DCinside launcher icons.

            A custom icon folder must contain one or more of the following directories:
            ${mipmapDirectories.keys.joinToString("\n") { "- $it" }}

            Each icon directory must contain exactly one launcher icon file:
            ${launcherIconFileNames.joinToString("\n") { "- $it" }}

            The expected launcher icon sizes are:
            ${mipmapDirectories.entries.joinToString("\n") { "- ${it.key}: ${it.value}" }}

            Optionally, you can provide a notification icon as either:
            - drawable/$notificationVectorIconFileName
            - drawable-<dpi>/${notificationRasterIconFileNames.joinToString(" or ")} with sizes:
            ${notificationIconDirectories.entries.joinToString("\n") { "- ${it.key}: ${it.value}" }}
        """.trimIndent(),
    ) {
        it == originalIconOptionValue || it == bundledIconOptionValue || !it.isNullOrBlank()
    }

    execute {
        val resolvedCustomName = customName?.trim()?.takeUnless(String::isEmpty)
        val resolvedCustomIcon = customIcon!!.trim()

        if (resolvedCustomName == null && resolvedCustomIcon == originalIconOptionValue) {
            return@execute Logger.getLogger(this::class.java.name)
                .warning("Custom app name is null and the original app icon is selected, skipping...")
        }

        if (resolvedCustomName != null) {
            removeStringsElements(
                arrayOf("app_name")
            )

            document("res/values/strings.xml").use { document ->
                mapOf(
                    "app_name" to resolvedCustomName,
                ).forEach { (k, v) ->
                    val stringElement = document.createElement("string")

                    stringElement.setAttribute("name", k)
                    stringElement.textContent = v

                    document.getElementsByTagName("resources").item(0)
                        .appendChild(stringElement)
                }
            }
        }

        when (resolvedCustomIcon) {
            originalIconOptionValue -> Unit
            bundledIconOptionValue -> {
                copyBundledCustomBrandingIcons()
                useBundledCustomBrandingIconsInManifest()
            }

            else -> copyCustomIcons(resolvedCustomIcon)
        }
    }
}
