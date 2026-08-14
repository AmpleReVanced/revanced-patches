package app.revanced.patches.kakaotalk.misc.tracker

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.getFreeRegisterProvider
import app.revanced.patches.kakaotalk.shared.fingerprints.KotlinUnitInstanceFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.patches.kakaotalk.misc.tracker.fingerprints.DisableSaveS2EventFingerprint
import app.revanced.patches.kakaotalk.misc.tracker.fingerprints.SendS2EventFingerprint
import app.revanced.util.smaliReference

@Suppress("unused")
val disableS2EventPatch = bytecodePatch(
    name = "Disable S2Event",
    description = "Disables Tracker",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        val disableSaveMethod = DisableSaveS2EventFingerprint.method
        val booleanRegister = disableSaveMethod.getFreeRegisterProvider(0, 1).getFreeRegister4Bit()
        disableSaveMethod.addInstructions(
            0,
            """
                const/4 v$booleanRegister, 0x0
                invoke-static {v$booleanRegister}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;
                move-result-object v$booleanRegister
                return-object v$booleanRegister
            """.trimIndent()
        )

        val unitClass = KotlinUnitInstanceFingerprint.classDef
        val unitInstance = unitClass.staticFields.singleOrNull { it.type == unitClass.type }
            ?: throw PatchException("Could not find Kotlin Unit instance")
        val sendMethod = SendS2EventFingerprint.method
        val unitRegister = sendMethod.getFreeRegisterProvider(0, 1).getFreeRegister4Bit()
        sendMethod.addInstructions(
            0,
            """
                sget-object v$unitRegister, ${unitInstance.smaliReference}
                return-object v$unitRegister
            """.trimIndent()
        )
    }
}
