package app.revanced.patches.dcinside.settings

import app.morphe.patcher.patch.ResourcePatchContext
import app.morphe.patcher.patch.resourcePatch
import app.morphe.util.inputStreamFromBundledResource
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE

private val USER_MEMO_PRESET_RESOURCES = arrayOf(
    "user_memo_preset_1.txt",
    "user_memo_preset_2.txt",
)

internal val userMemoResourcesPatch = resourcePatch {
    compatibleWith(COMPATIBILITY_DC_INSIDE)

    execute {
        copyUserMemoPresetResources()
    }
}

private fun ResourcePatchContext.copyUserMemoPresetResources() {
    USER_MEMO_PRESET_RESOURCES.forEach { resource ->
        val source = inputStreamFromBundledResource("dcinside", resource)
            ?: error("Could not find bundled resource: dcinside/$resource")
        val destination = this["res/raw/$resource"]

        destination.parentFile?.mkdirs()
        source.use { input ->
            destination.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}
