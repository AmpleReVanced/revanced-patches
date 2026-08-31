package app.revanced.patches.kakaotalk.interaction.send.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

// Shares its signature with Kotlin's trim extension, so it is matched through the call to
// KakaoTalk's own scan for the first char that its invalid char format check rejects.
internal object StripInvalidCharFormatSanitizerFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/CharSequence;",
    parameters = listOf("Ljava/lang/CharSequence;", "[C"),
    filters = listOf(
        methodCall(parameters = listOf("Ljava/lang/CharSequence;", "[C", "I"), returnType = "I"),
    ),
)