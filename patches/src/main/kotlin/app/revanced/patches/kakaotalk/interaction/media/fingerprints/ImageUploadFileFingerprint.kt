package app.revanced.patches.kakaotalk.interaction.media.fingerprints

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object ImageUploadFileFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "Ljava/io/File;",
    parameters = listOf("Ljava/io/File;", "I", "J"),
    custom = { _, classDef -> classDef.sourceFile == "ImageItemSendHelper.kt" },
)