package app.revanced.patches.kakaotalk.layout.tab.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall

internal object OpenChatListMoreItemFingerprint : Fingerprint(
    name = "getBindingType",
    parameters = emptyList(),
    returnType = "I",
    custom = { _, classDef -> classDef.sourceFile == "OpenChatListMoreItem.kt" },
)

internal object TrimOpenChatListForMoreButtonFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Ljava/util/List;", "Z", "Z"),
    filters = listOf(
        methodCall(
            definingClass = "Lkotlin/collections/CollectionsKt;",
            name = "take",
        ),
        methodCall(
            definingClass = "Ljava/util/List;",
            name = "clear",
            parameters = emptyList(),
            returnType = "V",
        ),
        methodCall(
            definingClass = "Ljava/util/List;",
            name = "addAll",
            parameters = listOf("Ljava/util/Collection;"),
            returnType = "Z",
        ),
    ),
    custom = { _, classDef -> classDef.sourceFile == "OpenChatTabFragment.kt" },
)
