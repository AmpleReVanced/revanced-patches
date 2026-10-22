package app.morphe.patches.util.resource

internal class RawResourceElement(
    private val text: String,
    val tag: String,
    val name: String,
    val start: Int,
    val innerStart: Int,
    val innerEnd: Int,
    val end: Int
) {
    val openTag get() = text.substring(start, innerStart)

    val inner get() = if (innerEnd < 0) null else text.substring(innerStart, innerEnd)
}

internal inline fun forEachRawResourceElement(
    text: String,
    action: (RawResourceElement) -> Unit
) {
    var pos = 0
    while (true) {
        val start = text.indexOf('<', pos)
        if (start < 0) return

        if (text.startsWith("<!--", start)) {
            pos = text.indexOf("-->", start + 4) + 3
            continue
        }

        val openEnd = text.indexOf('>', start)
        pos = openEnd + 1

        val nameStart = text.indexOf(" name=\"", start)
        if (text[start + 1] == '/' || text[start + 1] == '?' || nameStart < 0 || nameStart > openEnd) {
            continue
        }

        var tagEnd = start + 1
        while (text[tagEnd] > ' ' && text[tagEnd] != '>' && text[tagEnd] != '/') tagEnd++
        val tag = text.substring(start + 1, tagEnd)

        val nameValueStart = nameStart + 7
        val name = text.substring(nameValueStart, text.indexOf('"', nameValueStart))

        if (text[openEnd - 1] == '/') {
            action(RawResourceElement(text, tag, name, start, pos, -1, pos))
        } else {
            val closeStart = text.indexOf("</$tag>", openEnd)
            if (closeStart < 0) throw IllegalStateException("Missing closing tag for: $tag $name")
            val end = closeStart + tag.length + 3
            pos = end
            action(RawResourceElement(text, tag, name, start, openEnd + 1, closeStart, end))
        }
    }
}