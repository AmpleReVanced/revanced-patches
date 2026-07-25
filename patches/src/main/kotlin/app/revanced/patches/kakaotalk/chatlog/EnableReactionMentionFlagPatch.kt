package app.revanced.patches.kakaotalk.chatlog

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.getReference
import app.revanced.patches.kakaotalk.chatlog.fingerprints.ReactionMentionFlagFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.ReactionMentionFlagPreferenceFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.util.matches
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val enableReactionMentionFlagPatch = bytecodePatch(
    name = "Enable reaction mention flag",
    description = "Enables the reaction mention flag in KakaoTalk chat logs.",
    default = false // This patch is currently disabled due to issues with the KakaoTalk app.
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        val preferenceMethod = ReactionMentionFlagPreferenceFingerprint.originalMethod
        val method = ReactionMentionFlagFingerprint.matchAll()
            .singleOrNull { match -> match.originalMethod.hasReferenceTo(preferenceMethod) }
            ?.method
            ?: throw PatchException("Could not find reaction mention flag coroutine.")

        method.addInstructions(
            0,
            """
                sget-object p1, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;
                return-object p1
            """.trimIndent()
        )
    }
}

private fun Method.hasReferenceTo(method: Method) =
    implementation?.instructions?.any { instruction ->
        val reference = instruction.getReference<MethodReference>() ?: return@any false

        reference.definingClass == method.definingClass &&
            reference.matches(method)
    } == true
