package app.revanced.patches.kakaotalk.tracker.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

internal object TalkShareLogAsyncFlagFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = listOf("Lkotlin/coroutines/Continuation;"),
    returnType = "Ljava/lang/Object;",
    custom = { method, classDef ->
        classDef.sourceFile == "Available2.kt" &&
            method.implementation?.instructions?.any { instruction ->
                instruction.getReference<FieldReference>()?.name == "USE_TALK_SHARE_LOG"
            } == true
    },
)