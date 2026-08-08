package app.revanced.patches.shared.misc.native

import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.ResourcePatch
import app.morphe.patcher.patch.resourcePatch
import net.fornwall.jelf.ElfFile
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
            val elfFile = lazy { ElfFile.from(fileBytes) }
            fileReplacements.forEach { it.replaceIn(fileBytes, elfFile) }
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

    /**
     * Overwrite the 32-bit word at [offset] in the unique [fingerprint] match, guarded by
     * [expected]. The [fingerprint] may contain `??` wildcard bytes and is searched within
     * [symbol] when given, which keeps it short enough to avoid relocated operands.
     */
    fun replace(
        fingerprint: String,
        offset: Int = 0,
        symbol: String? = null,
        expected: String,
        replacement: String,
    ) {
        val expectedWord = expected.hexToWord()
        val replacementWord = replacement.hexToWord()
        val (pattern, mask) = fingerprint.hexToPattern()

        replacements += NativeReplacement(filePath, symbol, pattern, mask, offset) { word ->
            if (word != expectedWord) {
                throw PatchException("Unexpected native bytes in $filePath.")
            }
            replacementWord
        }
    }

    /**
     * Rewrite the AArch64 `TBZ`/`TBNZ` at [offset] in the unique [fingerprint] match into an
     * unconditional `B` to the same target (branch always taken). The displacement is read from
     * the matched instruction, so it need not be hard-coded.
     */
    fun forceBranch(
        fingerprint: String,
        offset: Int = 0,
        symbol: String? = null,
    ) {
        val (pattern, mask) = fingerprint.hexToPattern()

        replacements += NativeReplacement(filePath, symbol, pattern, mask, offset) { word ->
            testBitBranchToUnconditional(word)
                ?: throw PatchException("Expected an AArch64 TBZ/TBNZ at the patch site in $filePath.")
        }
    }

    internal fun build(): List<NativeReplacement> = replacements
}

internal class NativeReplacement(
    val filePath: String,
    private val symbol: String?,
    private val pattern: ByteArray,
    private val mask: BooleanArray,
    private val offset: Int,
    private val transform: (Int) -> Int,
) {
    fun replaceIn(fileBytes: ByteArray, elfFile: Lazy<ElfFile>) {
        val bounds = symbol?.let { elfFile.value.boundsOf(it, filePath) } ?: fileBytes.indices
        val matches = fileBytes.findAll(pattern, mask, bounds)

        if (matches.size != 1) {
            throw PatchException(
                "Expected exactly one native pattern in ${symbol ?: filePath}, found ${matches.size}.",
            )
        }

        val at = matches.single() + offset
        if (at < 0 || at + 4 > fileBytes.size) {
            throw PatchException("Native patch offset out of range in $filePath.")
        }

        val patched = transform(fileBytes.readWord(at))
        fileBytes.writeWord(at, patched)
    }
}

/** AArch64 `TBZ`/`TBNZ` [word] -> unconditional `B` to the same target, or `null` if not a TB*. */
private fun testBitBranchToUnconditional(word: Int): Int? {
    if ((word ushr 25) and 0x3F != 0x1B) return null // TBZ/TBNZ: bits[30:25] == 0b011011

    // Sign-extend imm14 (bits[18:5]) into B's 26-bit immediate.
    val imm14 = (word ushr 5) and 0x3FFF
    val imm26 = (imm14 shl 18) shr 18 and 0x03FFFFFF
    return 0x14000000 or imm26
}

/**
 * File offsets the exported [name] spans. Scoping a byte pattern to a symbol keeps it short
 * enough to leave out operands the linker rewrites between builds, such as `adrp`/`bl` targets
 * and `__LINE__` constants.
 */
private fun ElfFile.boundsOf(name: String, filePath: String): IntRange {
    val symbol = dynamicSymbolTableSection.symbols.firstOrNull { it.name == name }
        ?: throw PatchException("Could not find the symbol $name in $filePath.")

    return symbol.st_value.toInt() until (symbol.st_value + symbol.st_size).toInt()
}

private fun ByteArray.findAll(pattern: ByteArray, mask: BooleanArray, bounds: IntRange): List<Int> {
    if (pattern.isEmpty()) return emptyList()

    return (bounds.first..bounds.last - pattern.size + 1).filter { start ->
        pattern.indices.all { index -> !mask[index] || this[start + index] == pattern[index] }
    }
}

private fun ByteArray.readWord(at: Int): Int =
    (this[at].toInt() and 0xFF) or
        ((this[at + 1].toInt() and 0xFF) shl 8) or
        ((this[at + 2].toInt() and 0xFF) shl 16) or
        ((this[at + 3].toInt() and 0xFF) shl 24)

private fun ByteArray.writeWord(at: Int, value: Int) {
    this[at] = (value and 0xFF).toByte()
    this[at + 1] = ((value ushr 8) and 0xFF).toByte()
    this[at + 2] = ((value ushr 16) and 0xFF).toByte()
    this[at + 3] = ((value ushr 24) and 0xFF).toByte()
}

/** Parse a hex [String] into a byte pattern and match mask; whitespace is ignored, `??` is a wildcard. */
private fun String.hexToPattern(): Pair<ByteArray, BooleanArray> {
    val compact = filterNot(Char::isWhitespace)
    if (compact.length % 2 != 0) {
        throw PatchException("Hex pattern must have an even length.")
    }

    val size = compact.length / 2
    val bytes = ByteArray(size)
    val mask = BooleanArray(size)

    for (index in 0 until size) {
        val pair = compact.substring(index * 2, index * 2 + 2)
        if (pair == "??") continue

        mask[index] = true
        bytes[index] = try {
            pair.toInt(16).toByte()
        } catch (exception: NumberFormatException) {
            throw PatchException("Could not parse native hex pattern: $this", exception)
        }
    }

    return bytes to mask
}

/** Parse exactly four little-endian hex bytes into a 32-bit word. */
private fun String.hexToWord(): Int {
    val compact = filterNot(Char::isWhitespace)
    if (compact.length != 8) {
        throw PatchException("Expected a 4-byte (8 hex digit) word, got: $this")
    }

    return try {
        (0 until 4).fold(0) { acc, index ->
            val byte = compact.substring(index * 2, index * 2 + 2).toInt(16)
            acc or (byte shl (index * 8))
        }
    } catch (exception: NumberFormatException) {
        throw PatchException("Could not parse native word: $this", exception)
    }
}