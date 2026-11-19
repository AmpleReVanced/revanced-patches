package app.revanced.patches.kakaotalk.interaction.media.fingerprints

import app.morphe.patcher.Fingerprint
import app.revanced.util.parameterTypeNames
import com.android.tools.smali.dexlib2.AccessFlags

internal object ImageUploadFileFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "Ljava/io/File;",
    strings = listOf(".jpg"),
    custom = { method, classDef ->
        classDef.sourceFile == "ImageItemSendHelper.kt" &&
            method.parameterTypeNames.takeLast(3) == listOf("Ljava/io/File;", "I", "J")
    },
)