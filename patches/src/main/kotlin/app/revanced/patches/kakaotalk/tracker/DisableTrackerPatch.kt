package app.revanced.patches.kakaotalk.tracker

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

/**
 * Why doesn't this patch use a fingerprint?:
 *
 * Because due to the limitations of fingerprinting,
 * all methods using a certain method are matched, and if there is duplication in a class,
 * all methods in that class cannot be patched.
 */
@Suppress("unused")
val disableTrackerPatch = bytecodePatch(
    name = "Disable tracker",
    description = "Disables the tracker in KakaoTalk",
) {
    compatibleWith("com.kakao.talk"("25.8.2"))

    execute {
        val targetClass = "Lcom/kakao/talk/net/retrofit/service/TalkShareService;"
        val targetMethod = "log"

        classes.forEach { classDef ->
            classDef.methods.forEach { method ->
                val implementation = method.implementation ?: return@forEach
                var hasTargetCall = false

                implementation.instructions.forEach { instruction ->
                    if (instruction.opcode.name.startsWith("invoke")) {
                        val ref = (instruction as? ReferenceInstruction)?.reference as? MethodReference
                        if (ref?.definingClass == targetClass && ref.name == targetMethod) {
                            hasTargetCall = true
                        }
                    }
                }

                if (hasTargetCall) {
                    val mutableClass = proxy(classDef).mutableClass
                    val mutableMethod = mutableClass.methods
                        .first { it.name == method.name && it.returnType == method.returnType }

                    mutableMethod.addInstructions(
                        0,
                        """
                            return-void
                        """.trimIndent()
                    )
                }
            }
        }
    }
}