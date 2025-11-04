package app.revanced.patches.all.misc.shareduserid

import app.revanced.patcher.patch.*
import app.revanced.util.getNode
import org.w3c.dom.Element
import java.util.logging.Logger

lateinit var sharedUserIdOption: Option<String>

/**
 * Set the shared user ID to use.
 * If this is called multiple times, the first call will set the shared user ID.
 *
 * @param fallbackSharedUserId The shared user ID to use if the user has not already specified one.
 * @return The shared user ID that was set.
 * @throws OptionException.ValueValidationException If the shared user ID is invalid.
 */
fun setOrGetFallbackSharedUserId(fallbackSharedUserId: String): String {
    val sharedUserId = sharedUserIdOption.value!!

    return if (sharedUserId == sharedUserIdOption.default) {
        fallbackSharedUserId.also { sharedUserIdOption.value = it }
    } else {
        sharedUserId
    }
}

val addSharedUserIdPatch = resourcePatch(
    name = "Add shared user ID",
    description = "Adds or updates the sharedUserId attribute in the app's manifest. " +
            "This allows multiple apps with the same sharedUserId to share data and run in the same process. " +
            "Use with caution as this can affect app permissions and data isolation.",
    use = false,
) {
    sharedUserIdOption = stringOption(
        key = "sharedUserId",
        default = "revanced",
        values = mapOf("Default" to "revanced"),
        title = "Shared User ID",
        description = "The shared user ID to add to the app's manifest.",
        required = true,
    )

    finalize {
        document("AndroidManifest.xml").use { document ->
            val manifest = document.getNode("manifest") as Element
            val packageName = manifest.getAttribute("package")

            val sharedUserId = sharedUserIdOption.value

            if (sharedUserId == sharedUserIdOption.default) {
                return@finalize Logger.getLogger(this::class.java.name).warning(
                    "No shared user ID specified for '$packageName'. Skipping patch.",
                )
            }

            // Check if sharedUserId already exists
            val existingSharedUserId = manifest.getAttribute("android:sharedUserId")
            if (existingSharedUserId.isNotEmpty()) {
                Logger.getLogger(this::class.java.name).warning(
                    "Existing sharedUserId '$existingSharedUserId' will be replaced with '$sharedUserId'",
                )
            }

            manifest.setAttribute("android:sharedUserId", sharedUserId)

            Logger.getLogger(this::class.java.name).info(
                "Successfully set sharedUserId to '$sharedUserId' for package '$packageName'",
            )
        }
    }
}
