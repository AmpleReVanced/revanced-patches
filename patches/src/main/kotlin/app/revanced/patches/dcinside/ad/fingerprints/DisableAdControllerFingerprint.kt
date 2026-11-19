package app.revanced.patches.dcinside.ad.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.util.getReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object DisableAdControllerFingerprint : Fingerprint(
    isStatic = false,
    parameters = emptyList(),
    returnType = "V",
    filters = listOf(methodCall("Ljava/util/LinkedList;->pollFirst()Ljava/lang/Object;")),
    custom = { method, classDef ->
        method.implementation?.instructions?.any {
            it.getReference<MethodReference>()?.let { call ->
                call.name != "<init>" && call.parameterTypes == listOf(classDef.type) && call.returnType == "V"
            } == true
        } == true
    },
)