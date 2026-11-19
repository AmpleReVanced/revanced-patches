package app.revanced.patches.dcinside.layout.main

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionFilter
import app.morphe.patcher.methodCall
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resourceLiteral
import app.morphe.patcher.util.getReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

internal object HomeContentsRequestFingerprint : Fingerprint(
    strings = listOf("api_mainContents"),
    filters = listOf(InstructionFilter { _, instruction ->
        instruction.getReference<StringReference>()?.string in setOf("main_content_new.php", "main_content.php")
    }),
)

internal object MainAdapterFingerprint : Fingerprint(
    name = "onCreateViewHolder",
    parameters = listOf("Landroid/view/ViewGroup;", "I"),
    returnType = "L",
    filters = listOf(resourceLiteral(ResourceType.LAYOUT, "view_main_search_gallery_basic")),
)

internal object MainAdapterItemsUpdateFingerprint : Fingerprint(
    classFingerprint = MainAdapterFingerprint,
    parameters = listOf("Ljava/util/List;"),
    returnType = "V",
    filters = listOf(
        methodCall("Ljava/util/ArrayList;->clear()V"),
        methodCall("Ljava/util/ArrayList;->addAll(Ljava/util/Collection;)Z"),
        methodCall(name = "notifyDataSetChanged", parameters = emptyList(), returnType = "V"),
    ),
)
