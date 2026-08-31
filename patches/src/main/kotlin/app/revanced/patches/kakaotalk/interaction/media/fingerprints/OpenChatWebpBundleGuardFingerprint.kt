package app.revanced.patches.kakaotalk.interaction.media.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction

internal object OpenChatWebpBundleGuardFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf("I"),
    custom = { method, classDef ->
        classDef.sourceFile == "PickerUtils.kt" &&
            method.instructions
                .filter { it.opcode == Opcode.CONST_4 || it.opcode == Opcode.CONST_16 }
                .mapTo(mutableSetOf()) { (it as WideLiteralInstruction).wideLiteral } ==
                setOf(0L, 1L, 6L, 15L, 16L, 18L)
    },
)