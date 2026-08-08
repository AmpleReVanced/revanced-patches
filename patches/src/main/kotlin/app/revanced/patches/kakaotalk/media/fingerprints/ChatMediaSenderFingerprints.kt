package app.revanced.patches.kakaotalk.media.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private fun referencesOpenMulti(instructions: Iterable<Instruction>) =
    instructions.any {
        it.opcode == Opcode.SGET_OBJECT && it.getReference<FieldReference>()?.name == "OpenMulti"
    }

internal object ChatMediaSenderMediaItemBundleFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf("Z", "L", "Lcom/kakao/talk/model/media/MediaItem;"),
    custom = { method, classDef ->
        classDef.sourceFile == "ChatMediaSender.kt" && referencesOpenMulti(method.instructions)
    },
)

internal object ChatMediaSenderUriBundleFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf("Z", "L", "Landroid/net/Uri;"),
    custom = { method, classDef ->
        classDef.sourceFile == "ChatMediaSender.kt" && referencesOpenMulti(method.instructions)
    },
)