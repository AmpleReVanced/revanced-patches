package app.revanced.patches.kakaotalk.layout.tab

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.util.cloneMutable
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ThreeRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val EXTENSION_CLASS =
    "Lapp/revanced/extension/kakaotalk/patches/DisableNavigationBarAnimationPatch;"
private const val CONTINUATION = "Lkotlin/coroutines/Continuation;"

internal fun BytecodePatchContext.completeChatEntryWithoutAnimation(entry: Fingerprint) {
    val navigatorType = entry.originalMethod.parameterTypes[0]
    val navigateIndex = entry.method.indexOfFirstInstructionOrThrow {
        opcode == Opcode.INVOKE_INTERFACE && getReference<MethodReference>()?.let {
            it.definingClass == navigatorType && it.parameterTypes.size == 3 &&
                it.parameterTypes.last() == CONTINUATION
        } == true
    }
    val navigate = entry.method.getInstruction(navigateIndex).getReference<MethodReference>()!!
    val navigator = Fingerprint(
        parameters = navigate.parameterTypes.map { it.toString() },
        returnType = navigate.returnType,
        filters = listOf(
            methodCall(smali = "Landroidx/compose/runtime/snapshots/SnapshotStateList;->add(Ljava/lang/Object;)Z"),
            methodCall(parameters = listOf(CONTINUATION), returnType = "Ljava/lang/Object;"),
        ),
        custom = { _, classDef ->
            classDef.sourceFile == "ThreePaneScaffoldNavigator.kt" && navigatorType in classDef.interfaces
        },
    )
    val transition = navigator.instructionMatches[1].getMethodCalled()
    val animate = transition.implementation!!.instructions.mapNotNull {
        it.getReference<MethodReference>()
    }.single { it.parameterTypes.size == 7 && it.parameterTypes[4] == CONTINUATION }
    val stateType = animate.parameterTypes[0].toString()
    val targetType = animate.parameterTypes[1].toString()
    val stateGetter = navigator.classDef.methods.single {
        it.parameterTypes.isEmpty() && it.returnType == stateType
    }
    val targetGetter = navigator.classDef.methods.single {
        it.parameterTypes.isEmpty() && it.returnType == targetType
    }
    val snap = classDefBy(stateType).methods.single {
        it.parameterTypes == listOf(targetType, CONTINUATION) && it.returnType == "Ljava/lang/Object;"
    }
    val immediateNavigate = navigator.method.cloneMutable(name = "morphe_navigateWithoutAnimation").apply {
        val index = navigator.instructionMatches[1].index
        val call = getInstruction<FiveRegisterInstruction>(index)
        val registers = getFreeRegisterProvider(index, 2, call.registerC, call.registerD)
        val stateRegister = registers.getFreeRegister4Bit()
        val targetRegister = registers.getFreeRegister4Bit()
        replaceInstruction(index, "nop")
        addInstructions(
            index + 1,
            """
                invoke-virtual {v${call.registerC}}, $stateGetter
                move-result-object v$stateRegister
                invoke-virtual {v${call.registerC}}, $targetGetter
                move-result-object v$targetRegister
                invoke-virtual {v$stateRegister, v$targetRegister, v${call.registerD}}, $snap
            """,
        )
    }
    navigator.classDef.methods.add(immediateNavigate)

    val bridge = ImmutableMethod(
        entry.classDef.type,
        "morphe_navigateToDetail",
        (listOf(navigatorType) + navigate.parameterTypes + "Z").map {
            ImmutableMethodParameter(it.toString(), null, null)
        },
        navigate.returnType,
        AccessFlags.PRIVATE.value or AccessFlags.STATIC.value,
        null,
        null,
        MutableMethodImplementation(6),
    ).toMutable().apply {
        addInstructionsWithLabels(
            0,
            """
                if-eqz p4, :original
                instance-of v0, p0, ${navigator.classDef.type}
                if-eqz v0, :original
                check-cast p0, ${navigator.classDef.type}
                invoke-virtual {p0, p1, p2, p3}, $immediateNavigate
                move-result-object p0
                return-object p0
                :original
                invoke-interface {p0, p1, p2, p3}, $navigate
                move-result-object p0
                return-object p0
            """,
        )
    }
    entry.classDef.methods.add(bridge)
    entry.method.apply {
        val modeIndex = indexOfFirstInstructionOrThrow {
            getReference<MethodReference>()?.let {
                it.definingClass == entry.originalMethod.parameterTypes[1] && it.returnType == "Z"
            } == true
        }
        val usedRegisters = implementation!!.instructions.drop(modeIndex).flatMap {
            when (it) {
                is RegisterRangeInstruction -> (it.startRegister until it.startRegister + it.registerCount).toList()
                is FiveRegisterInstruction -> listOf(it.registerC, it.registerD, it.registerE, it.registerF, it.registerG)
                    .take(it.registerCount)
                is ThreeRegisterInstruction -> listOf(it.registerA, it.registerB, it.registerC)
                is TwoRegisterInstruction -> listOf(it.registerA, it.registerB)
                is OneRegisterInstruction -> listOf(it.registerA)
                else -> emptyList()
            }
        }.toSet()
        val flagRegister = (0 until implementation!!.registerCount - parameters.size).first {
            it !in usedRegisters
        }
        val modeRegister = getInstruction<OneRegisterInstruction>(modeIndex + 1).registerA
        val detailGetter = entry.instructionMatches[0].getMethodCalled().toString()
        val detailIndex = indexOfFirstInstructionOrThrow {
            getReference<MethodReference>()?.toString() == detailGetter
        } + 1
        val detailRegister = getInstruction<OneRegisterInstruction>(detailIndex).registerA
        val call = getInstruction<FiveRegisterInstruction>(navigateIndex)
        replaceInstruction(
            navigateIndex,
            "invoke-static {v${call.registerC}, v${call.registerD}, v${call.registerE}, v${call.registerF}, v$flagRegister}, $bridge",
        )
        addInstructions(
            detailIndex + 1,
            """
                invoke-static {v$flagRegister, v$detailRegister}, $EXTENSION_CLASS->shouldCompleteEntry(ZZ)Z
                move-result v$flagRegister
            """,
        )
        addInstructions(modeIndex + 2, "move v$flagRegister, v$modeRegister")
    }
}
