package app.revanced.patches.kakaotalk.branding

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patcher.patch.stringOption
import app.revanced.patches.shared.misc.mapping.resourceMappingPatch
import app.revanced.util.removeStringsElements
import java.util.logging.Logger

// TODO: Implement customize icon
@Suppress("unused")
val customBrandingPatch = resourcePatch(
    name = "Custom branding",
    description = "Customize app branding (name)",
    use = false
) {
    compatibleWith("com.kakao.talk"("25.9.2"))
    dependsOn(resourceMappingPatch)

    val customName by stringOption(
        key = "customName",
        title = "Custom app name",
        description = "Custom app name",
    )

    execute {
        if (customName == null) {
            return@execute Logger.getLogger(this::class.java.name)
                .warning("Custom app name is null or empty, skipping...")
        }

        removeStringsElements(
            arrayOf("app_name", "icon_name", "title_for_notification_service")
        )

        document("res/values/strings.xml").use { document ->
            mapOf(
                "app_name" to customName,
                "icon_name" to customName,
                "title_for_notification_service" to customName,
            ).forEach { (k, v) ->
                val stringElement = document.createElement("string")

                stringElement.setAttribute("name", k)
                stringElement.textContent = v

                document.getElementsByTagName("resources").item(0)
                    .appendChild(stringElement)
            }
        }
    }
}