package app.revanced.patches.kakaotalk.send.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

internal object EnableMarkdownFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("L", "Z", "Z"),
    filters = listOf(
        methodCall(returnType = "Lcom/kakao/talk/chat/ChatMessage;"),
        methodCall(
            parameters = listOf("Lorg/json/JSONObject;", "Z"),
            returnType = "Lorg/json/JSONObject;",
        ),
    ),
    custom = { _, classDef -> classDef.sourceFile == "InputViewModel.kt" },
)
