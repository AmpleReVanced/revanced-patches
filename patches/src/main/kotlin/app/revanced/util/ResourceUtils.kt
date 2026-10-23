package app.revanced.util

import app.morphe.patcher.patch.ResourcePatchContext
import java.io.File

fun ResourcePatchContext.removeStringsElements(
    replacements: Array<String>
) {
    decode("res/values*/strings.xml").forEach { path ->
        get(path).removeStringsElements(replacements)
    }
}

fun ResourcePatchContext.removeStringsElements(
    paths: Array<String>,
    replacements: Array<String>
) {
    paths.forEach { path ->
        get("res/$path/strings.xml").takeIf(File::isFile)?.removeStringsElements(replacements)
    }
}

private fun File.removeStringsElements(replacements: Array<String>) {
    var content = readText()
    replacements.forEach { replacement ->
        content = content.replace(
            Regex("""[ \t]*<string name="$replacement"[^>]*>.*?</string>\s*""", RegexOption.DOT_MATCHES_ALL),
            "",
        )
    }
    writeText(content)
}
