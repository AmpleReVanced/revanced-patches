package app.revanced.patches.kakaotalk.send.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object EnableMarkdownFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    custom = { method, classDef ->
        if (classDef.sourceFile != "InputViewModel.kt") {
            false
        } else {
            val parameterTypes = method.parameterTypes.toList()
            val references = method.implementation?.instructions
                ?.mapNotNull { it.getReference<MethodReference>() }
                ?: emptyList()

            parameterTypes.size == 3 &&
                    parameterTypes[1] == "Z" &&
                    parameterTypes[2] == "Z" &&
                    references.any { it.returnType == "Lcom/kakao/talk/chat/ChatMessage;" } &&
                    references.any {
                        it.returnType == "Lorg/json/JSONObject;" &&
                                it.parameterTypes == listOf("Lorg/json/JSONObject;", "Z")
                    }
        }
    }
)
