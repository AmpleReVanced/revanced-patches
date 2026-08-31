package app.revanced.patches.kakaotalk.layout.subdevice

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.returnEarly
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.util.smaliReference
import com.android.tools.smali.dexlib2.iface.Field

@Suppress("unused")
val showSubDeviceRestrictedMessagesPatch = bytecodePatch(
    name = "Show messages restricted to mobile",
    description = "Shows the content of AlimTalk, Leverage and Universal messages that are replaced " +
        "with a placeholder asking to open them on mobile while signed in on a sub device.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        val visibleCoverState = AlimtalkCoverStateFingerprint.instructionMatches.first().getFieldAccessed()

        AlimtalkCoverStateFingerprint.method.returnCoverState(visibleCoverState)
        LeverageCoverStateFingerprint.method.returnCoverState(visibleCoverState)

        UniversalMobileOnlyFingerprint.instructionMatches.first().getMethodCalled().returnEarly(false)
    }
}

private fun MutableMethod.returnCoverState(coverState: Field) {
    if (returnType != coverState.type) {
        throw PatchException("Unexpected cover state type in $definingClass->$name.")
    }

    val register = getFreeRegisterProvider(0, 1).getFreeRegister4Bit()
    addInstructions(
        0,
        """
            sget-object v$register, ${coverState.smaliReference}
            return-object v$register
        """.trimIndent(),
    )
}
