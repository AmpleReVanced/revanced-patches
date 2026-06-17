package app.revanced.patches.kakaotalk.tab.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

internal object SetupAdapterFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(),
    returnType = "V",
    custom = { method, classDef ->
        classDef.sourceFile == "OpenChatTabFragment.kt" &&
                !classDef.type.contains("$") &&
                method.implementation?.instructions?.let { instructions ->
                    val setAdapterReference = instructions.firstNotNullOfOrNull { instruction ->
                        instruction.getReference<MethodReference>()?.takeIf { reference ->
                            reference.definingClass == "Landroidx/recyclerview/widget/RecyclerView;" &&
                                    reference.name == "setAdapter" &&
                                    reference.parameterTypes.size == 1 &&
                                    reference.returnType == "V"
                        }
                    }
                    val adapterArrayType = setAdapterReference?.parameterTypes?.single()?.let { "[$it" }
                    val hasAdapterArray = instructions.any {
                        it.opcode == Opcode.NEW_ARRAY &&
                                it.getReference<TypeReference>()?.type == adapterArrayType
                    }
                    val hasScrollListener = instructions.any {
                        it.getReference<MethodReference>()?.let { reference ->
                            reference.definingClass == "Landroidx/recyclerview/widget/RecyclerView;" &&
                                    reference.name == "addOnScrollListener"
                        } == true
                    }

                    setAdapterReference != null && hasAdapterArray && hasScrollListener
                } == true
    }
)

internal object CommonChatRoomListAdapterClassFingerprint : Fingerprint(
    custom = { _, classDef ->
        classDef.sourceFile == "CommonChatRoomListAdapter.kt" &&
                !classDef.type.contains("$")
    }
)