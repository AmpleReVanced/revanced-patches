package app.revanced.patches.dcinside.dccon

import app.morphe.patcher.Fingerprint
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object PostDcconImageHandlerFingerprint : Fingerprint(
    definingClass = "Lcom/dcinside/app/wv",
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.STATIC, AccessFlags.FINAL),
    parameters = listOf("L", "L"),
    returnType = "V",
    strings = listOf(
        "dccon",
        "dccondetail",
        "groupIndex",
    ),
)

internal object ReplyDcconBindFingerprint : Fingerprint(
    definingClass = "Lcom/dcinside/app/read/holder",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("L", "Z"),
    returnType = "V",
    strings = listOf("dcconPack"),
)

internal object ReplyDcconAdapterBindFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE),
    parameters = listOf("L", "L", "L", "Ljava/lang/Boolean;"),
    returnType = "V",
    custom = custom@{ method, _ ->
        val references = method.implementation?.instructions
            ?.mapNotNull { it.getReference<MethodReference>() }
            ?: return@custom false

        references.any { reference ->
            reference.parameterTypes.map { it.toString() } == listOf("Lcom/dcinside/app/dccon/a;", "Z") &&
                reference.returnType == "V"
        } && references.any { reference ->
            reference.parameterTypes.map { it.toString() }.let { parameters ->
                parameters.contains("Lcom/dcinside/app/dccon/a;") &&
                    parameters.lastOrNull() == "Ljava/lang/Boolean;"
            } && reference.returnType == "V"
        }
    },
)
