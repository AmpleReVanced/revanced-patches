package app.revanced.patches.kakaotalk.chatlog

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.returnEarly
import app.revanced.patches.kakaotalk.chatlog.fingerprints.ForwardActionTypeFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.ForwardableChatLogFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.LinkForwardShortcutFingerprint
import app.revanced.patches.kakaotalk.chatlog.fingerprints.UNIVERSAL_ALIMTALK_MESSAGE_TYPE
import app.revanced.patches.kakaotalk.chatlog.fingerprints.forwardShortcutFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.util.smaliReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

@Suppress("unused")
val ignoreForwardRestrictionPatch = bytecodePatch(
    name = "Ignore forward restriction",
    description = "Ignores the restrictions that stop messages such as KakaoLink from being forwarded " +
        "to another chatroom.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        ForwardableChatLogFingerprint.instructionMatches
            .filter { it.instruction.opcode == Opcode.INVOKE_VIRTUAL }
            .forEach { it.getMethodCalled().returnEarly(true) }

        forwardShortcutFingerprint(LinkForwardShortcutFingerprint.originalMethod.name)
            .matchAll()
            .forEach { it.method.returnEarly(true) }

        ForwardActionTypeFingerprint.matchAll(2..2).forEach { match ->
            val messageType = match.instructionMatches[1].getFieldAccessed()
            val alimtalkMessageType = classDefBy(messageType.type).staticFields
                .single { it.name == UNIVERSAL_ALIMTALK_MESSAGE_TYPE }
            val add = match.instructionMatches[2]
            val (setRegister, typeRegister) = add.getInstruction<FiveRegisterInstruction>()
                .let { it.registerC to it.registerD }

            match.method.addInstructions(
                add.index + 1,
                """
                    sget-object v$typeRegister, ${alimtalkMessageType.smaliReference}
                    invoke-interface {v$setRegister, v$typeRegister}, Ljava/util/Set;->add(Ljava/lang/Object;)Z
                """.trimIndent(),
            )
        }
    }
}