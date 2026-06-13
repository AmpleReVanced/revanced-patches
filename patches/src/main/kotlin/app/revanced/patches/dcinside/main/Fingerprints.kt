package app.revanced.patches.dcinside.main

import app.morphe.patcher.Fingerprint
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object MainAdapterItemsUpdateFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    parameters = listOf("Ljava/util/List;"),
    returnType = "V",
    custom = custom@{ method, _ ->
        val references = method.implementation?.instructions
            ?.mapNotNull { it.getReference<MethodReference>() }
            ?: return@custom false

        references.any { reference ->
            reference.definingClass == "Ljava/util/ArrayList;" &&
                reference.name == "clear" &&
                reference.parameterTypes.isEmpty() &&
                reference.returnType == "V"
        } && references.any { reference ->
            reference.definingClass == "Ljava/util/ArrayList;" &&
                reference.name == "addAll" &&
                reference.parameterTypes.singleOrNull()?.toString() == "Ljava/util/Collection;" &&
                reference.returnType == "Z"
        } && references.any { reference ->
            reference.definingClass == "Landroidx/recyclerview/widget/RecyclerView\$Adapter;" &&
                reference.name == "notifyDataSetChanged" &&
                reference.parameterTypes.isEmpty() &&
                reference.returnType == "V"
        }
    },
)
