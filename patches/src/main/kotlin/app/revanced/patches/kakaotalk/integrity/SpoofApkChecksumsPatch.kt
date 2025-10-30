package app.revanced.patches.kakaotalk.integrity

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.integrity.fingerprints.getApkChecksumsFingerprint
import app.revanced.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Suppress("unused")
val spoofApkChecksumsPatch = bytecodePatch(
    name = "Spoof apk checksums",
    description = "Spoofs the apk checksums to pass integrity checks.",
) {
    compatibleWith("com.kakao.talk"("25.9.1"))

    execute {
        val getApkChecksumsMethod = getApkChecksumsFingerprint.method
        val sputObject = getApkChecksumsMethod.instructions.first {
            it.opcode == Opcode.SPUT_OBJECT
        }

        val apkChecksums =
            ::javaClass.javaClass.classLoader.getResourceAsStream("kakaotalk/apkChecksum")?.bufferedReader()?.readText()
                ?: throw PatchException("Failed to load original signature resource.")

        val variable = sputObject.getReference<FieldReference>()?.name ?: throw PatchException("Failed to get apk checksum field name.")

        getApkChecksumsMethod.addInstructions(
            sputObject.location.index + 1,
            """
                const-string v0, "$apkChecksums"
                invoke-virtual {v0}, Ljava/lang/String;->toCharArray()[C
                move-result-object v0
                sput-object v0, Lcom/kakao/talk/util/AbuseDetectUtil;->$variable:[C
            """.trimIndent()
        )
    }
}