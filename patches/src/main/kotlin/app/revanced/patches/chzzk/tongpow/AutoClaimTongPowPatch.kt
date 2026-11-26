package app.revanced.patches.chzzk.tongpow

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import app.revanced.patches.chzzk.shared.Constants.COMPATIBILITY_CHZZK
import app.revanced.util.argumentRegister
import app.revanced.util.smaliReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.BuilderOffsetInstruction
import com.android.tools.smali.dexlib2.builder.BuilderSwitchPayload
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private fun MutableMethod.literalArgument(index: Int, parameterIndex: Int): Int {
    val register = getInstruction(index).argumentRegister(parameterIndex)
    val switches = buildMap {
        instructions.forEachIndexed { switchIndex, instruction ->
            if (instruction.opcode != Opcode.PACKED_SWITCH && instruction.opcode != Opcode.SPARSE_SWITCH) return@forEachIndexed
            val payload = getInstruction((instruction as BuilderOffsetInstruction).target.location.index)
            (payload as BuilderSwitchPayload).switchElements.forEach { put(it.target.location.index, switchIndex) }
        }
    }

    var current = index - 1
    while (current >= 0) {
        val instruction = getInstruction(current)
        if (instruction.opcode.setsRegister() && (instruction as? OneRegisterInstruction)?.registerA == register) {
            return (instruction as? NarrowLiteralInstruction)?.narrowLiteral ?: break
        }
        current = (switches[current] ?: current) - 1
    }
    throw PatchException("Could not resolve argument $parameterIndex of instruction $index in $this.")
}

@Suppress("unused")
val autoClaimTongPowPatch = patch(
    name = "Auto claim TongPow",
    description = "Automatically claims CHZZK TongPow rewards when they become available while watching streams.",
) {
    compatibleWith(COMPATIBILITY_CHZZK)

    execute {
        val chatViewModel = TongPowChatEventFingerprint.classDef
        val (popup, dispatch) = TongPowChatEventFingerprint.instructionMatches
        val flow = popup.getInstruction<RegisterRangeInstruction>().startRegister
        val channel = flow + 1
        val scope = flow + 2
        val claim = flow + 3
        val callback = flow + 4
        val constant = flow + 5
        if (constant > 15) throw PatchException("Unexpected TongPow popup registers.")

        TongPowManualClaimFingerprint.let {
            val calls = it.instructionMatches.map { match -> match.methodReference.smaliReference }
            val (claimCall, _, _, success, error) = it.instructionMatches
            val callbackIndex = it.method.indexOfFirstInstructionReversedOrThrow(
                claimCall.index,
                methodCall(name = "<init>", parameters = listOf(chatViewModel.type, "I")),
            )
            val callbackConstructor = it.method.getInstruction(callbackIndex).getReference<MethodReference>()!!
            val service = chatViewModel.fields.single { field ->
                field.type == claimCall.methodReference.definingClass
            }

            TongPowChatEventFingerprint.method.addInstructionsWithLabels(
                dispatch.index + 1,
                """
                    if-eqz v$channel, :skip
                    if-eqz v$claim, :skip
                    iget-object v$scope, p0, ${service.smaliReference}
                    invoke-interface {v$scope, v$channel, v$claim}, ${calls[0]}
                    move-result-object v$flow
                    invoke-static {v$flow}, ${calls[1]}
                    move-result-object v$flow
                    invoke-virtual {p0}, ${calls[2]}
                    move-result-object v$scope
                    new-instance v$callback, ${callbackConstructor.definingClass}
                    const/16 v$constant, ${it.method.literalArgument(callbackIndex, 1)}
                    invoke-direct {v$callback, p0, v$constant}, ${callbackConstructor.smaliReference}
                    new-instance v$channel, ${success.methodReference.definingClass}
                    const/4 v$claim, 0x0
                    const/16 v$constant, ${it.method.literalArgument(success.index, 2)}
                    invoke-direct {v$channel, v$callback, v$claim, v$constant}, ${calls[3]}
                    new-instance v$callback, ${error.methodReference.definingClass}
                    const/16 v$constant, ${it.method.literalArgument(error.index, 0)}
                    invoke-direct {v$callback, v$constant}, ${calls[4]}
                    invoke-static {v$flow, v$scope, v$channel, v$callback}, ${calls[5]}
                    move-result-object v$flow
                    invoke-virtual {p0, v$flow}, ${calls[6]}
                """,
                ExternalLabel("skip", TongPowChatEventFingerprint.method.getInstruction(dispatch.index + 1)),
            )
        }
    }
}
