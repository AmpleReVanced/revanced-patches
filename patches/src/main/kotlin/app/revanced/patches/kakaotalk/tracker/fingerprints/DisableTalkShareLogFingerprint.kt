package app.revanced.patches.kakaotalk.tracker.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.util.hasFieldReference
import com.android.tools.smali.dexlib2.AccessFlags

internal object TalkShareLogAsyncFlagFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = listOf("Lkotlin/coroutines/Continuation;"),
    returnType = "Ljava/lang/Object;",
    custom = { method, classDef ->
        classDef.sourceFile == "Available2.kt" &&
                method.hasFieldReference("Lbr/c\$b;", "USE_TALK_SHARE_LOG")
    }
)