package app.revanced.patches.kakaotalk.packagename

import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.util.findMutableMethodOf
import app.revanced.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction21c
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference

@Suppress("unused")
val changePackageNameInSourcePatch = bytecodePatch(
    name = "Change package name in source",
    description = "Changes the package name in the source code of KakaoTalk.",
    use = false,
) {
//    dependsOn(changePackageNamePatch)

    execute {
//        val newPackageName = if (packageNameOption.value == packageNameOption.default) {
//            "com.kakao.talk.revanced"
//        } else {
//            packageNameOption.value!!
//        }
        val newPackageName = "com.kakao.talk.revanced"

        val newStringReference = ImmutableStringReference(newPackageName)

        classes.forEach { classDef ->
            classDef.methods.forEach { method ->
                val insns = method.implementation?.instructions ?: return@forEach

                for ((i, insn) in insns.withIndex()) {
                    if (insn is Instruction21c &&
                        insn.opcode == Opcode.CONST_STRING &&
                        insn.getReference<StringReference>()!!.string == "com.kakao.talk") {

                        val mutableMethod = proxy(classDef).mutableClass.findMutableMethodOf(method)

                        mutableMethod.instructions
                            .filterIsInstance<BuilderInstruction21c>()
                            .filter { it.opcode == Opcode.CONST_STRING &&
                                it.getReference<StringReference>()!!.string == "com.kakao.talk" }
                            .forEach {
                                val idx = mutableMethod.instructions.indexOf(it)

                                println("Replacing package name in method $mutableMethod at index $idx")

                                mutableMethod.replaceInstruction(
                                    idx,
                                    BuilderInstruction21c(
                                        Opcode.CONST_STRING,
                                        it.registerA,
                                        ImmutableStringReference(newPackageName)
                                    )
                                )
                            }
                    }
                }
            }
        }
    }
}