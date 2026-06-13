package app.revanced.patches.chzzk.p2p

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.getReference
import app.morphe.util.returnEarly
import app.revanced.patches.chzzk.shared.Constants.COMPATIBILITY_CHZZK
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Suppress("unused")
val disableP2PPatch = bytecodePatch(
    name = "Disable P2P",
    description = "Disables CHZZK peer-assisted grid streaming and forces CDN playback.",
) {
    compatibleWith(COMPATIBILITY_CHZZK)

    execute {
        val methods = PeerNetworkConfigurationFingerprint.classDef.methods
            .filter { method ->
                if (method.parameterTypes.isNotEmpty() || method.returnType != "Z") {
                    return@filter false
                }

                val instructions = method.implementation?.instructions?.iterator()
                    ?: return@filter false

                if (!instructions.hasNext()) return@filter false
                val fieldGet = instructions.next()
                if (!instructions.hasNext()) return@filter false
                val returnValue = instructions.next()
                if (instructions.hasNext()) return@filter false

                val fieldReference = fieldGet.getReference<FieldReference>() ?: return@filter false
                val fieldGetRegister = (fieldGet as? TwoRegisterInstruction)?.registerA
                    ?: return@filter false
                val returnRegister = (returnValue as? OneRegisterInstruction)?.registerA
                    ?: return@filter false

                fieldGet.opcode == Opcode.IGET_BOOLEAN &&
                    returnValue.opcode == Opcode.RETURN &&
                    fieldReference.definingClass == PEER_NETWORK_CONFIGURATION_CLASS &&
                    fieldReference.type == "Z" &&
                    fieldGetRegister == returnRegister
            }

        if (methods.isEmpty()) {
            throw PatchException("Could not find PeerNetworkConfiguration enabled getter.")
        }

        methods.forEach { method ->
            method.returnEarly(false)
        }
    }
}