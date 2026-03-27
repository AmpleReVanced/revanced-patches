package app.revanced.patches.kakaotalk.tab

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.revanced.patches.kakaotalk.tab.fingerprints.commonChatRoomListAdapterClassFingerprint
import app.revanced.patches.kakaotalk.tab.fingerprints.initViewModelFingerprint
import app.revanced.patches.kakaotalk.tab.fingerprints.setupAdapterFingerprint
import app.morphe.util.getReference
import app.revanced.patches.kakaotalk.shared.Constants.COMPATIBILITY_KAKAO
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

@Suppress("unused")
val disableCommunityTabPatch = bytecodePatch(
    name = "Disable Community Tab",
    description = "Disables Community Tab",
) {
    compatibleWith(COMPATIBILITY_KAKAO)

    execute {
        setupAdapterFingerprint.method.apply {
            val callSetAdapter = instructions.first {
                it.opcode == Opcode.INVOKE_VIRTUAL && it.getReference<MethodReference>()?.name == "setAdapter"
            }

            val newInstanceRecyclerView = instructions.first {
                it.opcode == Opcode.NEW_INSTANCE && it.getReference<TypeReference>()?.type?.startsWith("Landroidx/recyclerview") == true
            }
            val invokeVirtual = instructions.first {
                it.opcode == Opcode.INVOKE_VIRTUAL && it.getReference<MethodReference>()?.name == "addOnScrollListener"
            }

            removeInstructions(
                newInstanceRecyclerView.location.index,
                invokeVirtual.location.index - newInstanceRecyclerView.location.index + 1
            )

            val adapterClass = commonChatRoomListAdapterClassFingerprint.classDef.type

            addInstructions(
                newInstanceRecyclerView.location.index,
                """
                    iget-object v5, v0, Lcom/kakao/talk/openlink/maintab/presentation/OpenChatTabFragment;->O:$adapterClass
                    invoke-virtual {v2, v5}, ${callSetAdapter.getReference<MethodReference>()}
                """.trimIndent()
            )

            initViewModelFingerprint.method.apply {
                removeInstructions(
                    instructions.filter { it.opcode == Opcode.INVOKE_VIRTUAL }[4].location.index,
                    1
                )
            }
        }
    }
}