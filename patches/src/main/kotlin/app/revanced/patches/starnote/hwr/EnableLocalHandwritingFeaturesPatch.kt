package app.revanced.patches.starnote.hwr

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.RawResourcePatch
import app.morphe.patcher.patch.ResourcePatchContext
import app.morphe.patcher.patch.rawResourcePatch
import app.revanced.patches.starnote.premium.unlockPremiumStatePatch
import app.revanced.patches.starnote.shared.Constants.COMPATIBILITY_STARNOTE
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File

private const val ENGINE_ML = "ML"

@Suppress("unused")
val enableLocalHandwritingSearchPatch = localHandwritingPatch(
    name = "Enable local handwriting search",
    description = "Uses the on-device ML engine to index and search handwriting.",
    functionKey = "hwr_search",
)

@Suppress("unused")
val useLocalMlForSmartHwrPatch = localHandwritingPatch(
    name = "Use local ML for Smart HWR",
    description = "Uses the on-device ML engine instead of SuNia for Smart HWR.",
    functionKey = "hwr_smart",
)

private fun localHandwritingPatch(
    name: String,
    description: String,
    functionKey: String,
): RawResourcePatch = rawResourcePatch(
    name = name,
    description = description,
) {
    compatibleWith(COMPATIBILITY_STARNOTE)

    dependsOn(unlockPremiumStatePatch)

    execute {
        rawResource("hwr_recognizer_config.json").updateJson { config ->
            val entries = config.getAsJsonArray("engineEntryList")
                ?: throw PatchException("Missing handwriting engine entries")
            val matches = entries.filter { it.asJsonObject.get("functionKey")?.asString == functionKey }
            if (matches.size != 1) {
                throw PatchException("Expected one handwriting function mapping, found ${matches.size}")
            }
            matches.single().asJsonObject.addProperty("engineName", ENGINE_ML)
        }
    }
}

private fun ResourcePatchContext.rawResource(name: String): File {
    val matches = get("assets").parentFile.parentFile.walkTopDown()
        .filter { it.isFile && it.invariantSeparatorsPath.endsWith("/res/raw/$name") }
        .toList()
    if (matches.size != 1) {
        throw PatchException("Expected one raw resource, found ${matches.size}: $name")
    }
    return matches.single()
}

private fun File.updateJson(block: (JsonObject) -> Unit) {
    val json = JsonParser.parseString(readText()).asJsonObject
    block(json)
    writeText(GsonBuilder().setPrettyPrinting().create().toJson(json))
}