package app.revanced.util

import app.morphe.patcher.patch.ResourcePatchContext

//
// Ample specific code
//

fun ResourcePatchContext.removeStringsElements(
    replacements: Array<String>
) {
    val resourceDirectory = get("res")
    val languagePaths = resourceDirectory.listFiles()
        ?.filter { it.isDirectory && it.name.startsWith("values") }
        ?.filter { it.resolve("strings.xml").exists() }
        ?.map { it.name }
        ?.toTypedArray()
        ?: emptyArray()

    removeStringsElements(languagePaths, replacements)
}

fun ResourcePatchContext.removeStringsElements(
    paths: Array<String>,
    replacements: Array<String>
) {
    paths.forEach { path ->
        val resourceDirectory = get("res")
        val targetXmlPath = resourceDirectory.resolve(path).resolve("strings.xml")

        if (targetXmlPath.exists()) {
            val targetXml = get("res/$path/strings.xml")
            var content = targetXml.readText()
            replacements.forEach { replacement ->
                content = content.replace(
                    Regex("""[ \t]*<string name="$replacement"[^>]*>.*?</string>\s*""", RegexOption.DOT_MATCHES_ALL),
                    ""
                )
            }
            targetXml.writeText(content)
        }
    }
}
