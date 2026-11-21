package app.revanced.util

import app.morphe.patcher.patch.decode
import app.morphe.patcher.patch.resourceFile
import java.io.File

fun removeStringsElements(
    replacements: Array<String>
) {
    decode("res/values*/strings.xml").forEach { path ->
        resourceFile(path).removeStringsElements(replacements)
    }
}

fun removeStringsElements(
    paths: Array<String>,
    replacements: Array<String>
) {
    paths.forEach { path ->
        resourceFile("res/$path/strings.xml").takeIf(File::isFile)?.removeStringsElements(replacements)
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
