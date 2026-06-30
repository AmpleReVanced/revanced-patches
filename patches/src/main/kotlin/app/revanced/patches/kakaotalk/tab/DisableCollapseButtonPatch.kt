package app.revanced.patches.kakaotalk.tab

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.getReference
import app.morphe.util.returnEarly
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import app.revanced.patches.kakaotalk.tab.fingerprints.OpenChatListMoreItemFingerprint
import app.revanced.patches.kakaotalk.tab.fingerprints.SetupAdapterFingerprint
import app.revanced.patches.kakaotalk.tab.fingerprints.TrimOpenChatListForMoreButtonFingerprint
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

@Suppress("unused")
val disableCollapseButtonPatch = bytecodePatch(
    name = "Disable Collapse Button",
    description = "Disable collapse button on OpenChatList",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        val openChatListMoreItemType = OpenChatListMoreItemFingerprint.classDef.type

        TrimOpenChatListForMoreButtonFingerprint.method.returnEarly()

        SetupAdapterFingerprint.classDef.methods.firstOrNull { method ->
            method.returnType == "V" &&
                    method.parameterTypes == listOf("Ljava/util/List;", "Z") &&
                    method.implementation?.instructions?.any { instruction ->
                        instruction.opcode == Opcode.NEW_INSTANCE &&
                                instruction.getReference<TypeReference>()?.type == openChatListMoreItemType
                    } == true
        }?.returnEarly() ?: throw PatchException("Could not find OpenChat list more item insertion method")
    }
}