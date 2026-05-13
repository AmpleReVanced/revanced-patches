package app.revanced.patches.kakaotalk.tracker.fingerprints

import app.morphe.patcher.Fingerprint
import app.revanced.util.hasFieldReference
import com.android.tools.smali.dexlib2.AccessFlags

internal object TalkShareLogAsyncFlagFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = listOf("Lkotlin/coroutines/Continuation;"),
    returnType = "Ljava/lang/Object;",
    custom = { method, classDef ->
        classDef.sourceFile == "Available2.kt" &&
                method.hasFieldReference("${classDef.type.split("/")[0]}/c\$b;", "USE_TALK_SHARE_LOG")
    }
)