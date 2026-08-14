package app.revanced.patches.kakaotalk.misc.integrity

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.getReference
import app.revanced.patches.kakaotalk.misc.integrity.fingerprints.GetApkChecksumsFingerprint
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.util.smaliReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Suppress("unused")
val spoofApkChecksumsPatch = bytecodePatch(
    name = "Spoof apk checksums",
    description = "Spoofs the apk checksums to pass integrity checks.",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        val getApkChecksumsMethod = GetApkChecksumsFingerprint.method
        val sputObject = getApkChecksumsMethod.instructions.first {
            it.opcode == Opcode.SPUT_OBJECT
        }

        val apkChecksums =
            ::javaClass.javaClass.classLoader.getResourceAsStream("kakaotalk/apkChecksum")?.bufferedReader()?.readText()?.trim()
                ?: throw PatchException("Failed to load original signature resource.")

        val checksumField = sputObject.getReference<FieldReference>()
            ?: throw PatchException("Failed to get apk checksum field name.")
        val insertionIndex = sputObject.location.index + 1
        val register = getApkChecksumsMethod.getFreeRegisterProvider(insertionIndex, 1)
            .getFreeRegister4Bit()

        getApkChecksumsMethod.addInstructions(
            insertionIndex,
            """
                const-string v$register, "$apkChecksums"
                invoke-virtual {v$register}, Ljava/lang/String;->toCharArray()[C
                move-result-object v$register
                sput-object v$register, ${checksumField.smaliReference}
            """.trimIndent()
        )
    }
}