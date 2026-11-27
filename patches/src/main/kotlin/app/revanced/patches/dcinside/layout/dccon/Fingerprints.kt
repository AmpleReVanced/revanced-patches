package app.revanced.patches.dcinside.layout.dccon

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.classDefByOrNull

internal object PostDcconImageHandlerFingerprint : Fingerprint(
    isStatic = true,
    parameters = listOf("Landroid/content/Context;", "L", "Z", "I", "L", "Z", "Z", "I"),
    returnType = "L",
    strings = listOf("dccondetail", "groupIndex"),
)

internal object ReplyDcconBindFingerprint : Fingerprint(
    isStatic = false,
    parameters = listOf("L", "Z"),
    returnType = "V",
    filters = listOf(methodCall(parameters = emptyList(), returnType = "Lcom/dcinside/app/dccon/DcconPiece;")),
    custom = { method, classDef ->
        classDef.fields.count { it.type == "Landroid/widget/ImageView;" } >= 2 &&
            classDefByOrNull(method.parameterTypes[0].toString())?.fields?.count {
                it.type == "Lcom/dcinside/app/dccon/DcconPiece;"
            } == 2
    },
)

internal object ReplyDcconAdapterBindFingerprint : Fingerprint(
    parameters = listOf("L", "L", "L", "Ljava/lang/Boolean;"),
    returnType = "V",
    filters = listOf(methodCall(ReplyDcconBindFingerprint.originalMethod)),
)
