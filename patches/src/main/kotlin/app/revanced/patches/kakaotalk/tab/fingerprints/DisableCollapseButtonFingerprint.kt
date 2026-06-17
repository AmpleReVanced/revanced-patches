package app.revanced.patches.kakaotalk.tab.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object OpenChatListMoreItemFingerprint : Fingerprint(
    custom = { _, classDef ->
        classDef.sourceFile == "OpenChatListMoreItem.kt" &&
                !classDef.type.contains("$")
    }
)

internal object TrimOpenChatListForMoreButtonFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Ljava/util/List;", "Z", "Z"),
    custom = { method, classDef ->
        classDef.sourceFile == "OpenChatTabFragment.kt" &&
                method.implementation?.instructions?.let { instructions ->
                    instructions.any { instruction ->
                        instruction.getReference<MethodReference>()?.let { reference ->
                            reference.definingClass == "Lkotlin/collections/CollectionsKt;" &&
                                    reference.name == "take"
                        } == true
                    } &&
                            instructions.any { instruction ->
                                instruction.getReference<MethodReference>()?.let { reference ->
                                    reference.definingClass == "Ljava/util/List;" &&
                                            reference.name == "clear"
                                } == true
                            } &&
                            instructions.any { instruction ->
                                instruction.getReference<MethodReference>()?.let { reference ->
                                    reference.definingClass == "Ljava/util/List;" &&
                                            reference.name == "addAll"
                                } == true
                            }
                } == true
    }
)