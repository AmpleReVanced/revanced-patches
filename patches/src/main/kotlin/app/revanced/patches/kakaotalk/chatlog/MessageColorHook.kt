package app.revanced.patches.kakaotalk.chatlog

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.revanced.patches.kakaotalk.chatlog.fingerprints.GetDeletedColorFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.GetHiddenColorFingerprint

context(patchContext: BytecodePatchContext)
internal fun applyDeletedHiddenColors(deletedColorText: String, hiddenColorText: String) {
    val deletedLiteral = parseArgb32ToSmaliIntLiteral(deletedColorText)
    val hiddenLiteral = parseArgb32ToSmaliIntLiteral(hiddenColorText)

    GetDeletedColorFingerprint.method.replaceInstruction(0, "const v0, $deletedLiteral")
    GetHiddenColorFingerprint.method.replaceInstruction(0, "const v0, $hiddenLiteral")
}

private fun parseArgb32ToSmaliIntLiteral(input: String): String {
    val trimmed = input.trim().replace("_", "")
    val value = when {
        trimmed.startsWith("0x", ignoreCase = true) -> trimmed.substring(2).toLong(16)
        trimmed.startsWith("-0x", ignoreCase = true) -> -trimmed.substring(3).toLong(16)
        else -> trimmed.toLong()
    }

    return toSmaliIntLiteral((value and 0xFFFF_FFFFL).toInt())
}

private fun toSmaliIntLiteral(value: Int): String {
    return if (value < 0) {
        "-0x${kotlin.math.abs(value.toLong()).toString(16)}"
    } else {
        "0x${value.toString(16)}"
    }
}