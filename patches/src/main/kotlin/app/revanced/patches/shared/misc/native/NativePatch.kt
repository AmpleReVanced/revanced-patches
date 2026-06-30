package app.revanced.patches.shared.misc.native

import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.ResourcePatch
import app.morphe.patcher.patch.resourcePatch
import java.io.File

internal fun nativePatch(
    compatibility: Compatibility,
    block: NativePatchBuilder.() -> Unit,
): ResourcePatch = resourcePatch {
    compatibleWith(compatibility)

    execute {
        val replacements = NativePatchBuilder().apply(block).build()
        var patchedFiles = 0

        replacements.groupBy { it.filePath }.forEach { (filePath, fileReplacements) ->
            val file = File(get(filePath).toString())
            if (!file.exists()) return@forEach

            val fileBytes = file.readBytes()
            fileReplacements.forEach { it.replaceIn(fileBytes) }
            file.writeBytes(fileBytes)
            patchedFiles++
        }

        if (patchedFiles == 0) {
            throw PatchException("Could not find any native files to patch.")
        }
    }
}

internal class NativePatchBuilder {
    private val replacements = mutableListOf<NativeReplacement>()

    fun file(
        path: String,
        block: NativeFilePatchBuilder.() -> Unit,
    ) {
        replacements += NativeFilePatchBuilder(path).apply(block).build()
    }

    internal fun build(): List<NativeReplacement> = replacements
}

internal class NativeFilePatchBuilder(
    private val filePath: String,
) {
    private val replacements = mutableListOf<NativeReplacement>()

    fun replace(
        fingerprint: String,
        offset: Int = 0,
        expected: String,
        replacement: String,
    ) {
        replacements += NativeReplacement(
            filePath = filePath,
            fingerprint = fingerprint.hexToBytes(),
            offset = offset,
            expected = expected.hexToBytes(),
            replacement = replacement.hexToBytes(),
        )
    }

    internal fun build(): List<NativeReplacement> = replacements
}

internal class NativeReplacement(
    val filePath: String,
    private val fingerprint: ByteArray,
    private val offset: Int,
    private val expected: ByteArray,
    private val replacement: ByteArray,
) {
    fun replaceIn(fileBytes: ByteArray) {
        val matches = fileBytes.findAll(fingerprint)

        if (matches.size != 1) {
            throw PatchException(
                "Expected exactly one native pattern in $filePath, found ${matches.size}.",
            )
        }

        val replacementOffset = matches.single() + offset
        val actual = fileBytes.copyOfRange(replacementOffset, replacementOffset + expected.size)
        if (!actual.contentEquals(expected)) {
            throw PatchException("Unexpected native bytes in $filePath.")
        }

        replacement.copyInto(fileBytes, replacementOffset)
    }
}

private fun ByteArray.findAll(pattern: ByteArray): List<Int> {
    if (pattern.isEmpty() || size < pattern.size) return emptyList()

    return (0..size - pattern.size).filter { offset ->
        pattern.indices.all { index -> this[offset + index] == pattern[index] }
    }
}

private fun String.hexToBytes(): ByteArray {
    val hex = filterNot(Char::isWhitespace)
    if (hex.length % 2 != 0) {
        throw PatchException("Hex string must have an even length.")
    }

    return try {
        ByteArray(hex.length / 2) { index ->
            hex.substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
    } catch (exception: NumberFormatException) {
        throw PatchException("Could not parse native hex pattern: $this", exception)
    }
}