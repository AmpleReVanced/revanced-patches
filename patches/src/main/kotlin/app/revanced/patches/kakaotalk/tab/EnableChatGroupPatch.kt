package app.revanced.patches.kakaotalk.tab

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.tab.fingerprints.checkChatGroupFeatureFingerprint
import app.revanced.patches.kakaotalk.tab.fingerprints.checkOpenChatTabFeature
import app.revanced.patches.kakaotalk.tab.fingerprints.listChatRoomSettingsItems
import app.revanced.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11n
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction22c
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val enableChatGroupPatch = bytecodePatch(
    name = "Enable Chat Group",
    description = "An enable Chat Group Patch",
    use = false // Disabled until further testing
) {
    compatibleWith("com.kakao.talk"("25.9.0"))

    execute {
        val checkChatGroupMethod = checkChatGroupFeatureFingerprint.method

        val iget = checkChatGroupMethod.instructions.first { it.opcode == Opcode.IGET_OBJECT } as BuilderInstruction22c

        checkChatGroupMethod.addInstructions(
            0,
            """
                iget-object v0, p0, ${iget.getReference<FieldReference>()?.definingClass}->a:Landroid/content/SharedPreferences;
                const-string v1, "enable_chatroom_group"
                const/4 v2, 0x0
                invoke-interface {v0, v1, v2}, Landroid/content/SharedPreferences;->getBoolean(Ljava/lang/String;Z)Z
                move-result v0
                return v0
            """.trimIndent()
        )

        val checkMethodName = checkOpenChatTabFeature.method.name
        val listItems = listChatRoomSettingsItems.method
        val insns = listItems.instructions

        val invokeStaticIdx = insns
            .indexOfFirst { it.opcode == Opcode.INVOKE_STATIC && it.getReference<MethodReference>()?.name == checkMethodName }

        val moveResult = listItems.getInstruction(invokeStaticIdx + 1) as BuilderInstruction11x

        listItems.addInstruction(
            invokeStaticIdx + 2,
            BuilderInstruction11n(
                Opcode.CONST_4,
                moveResult.registerA,
                0
            )
        )
    }
}