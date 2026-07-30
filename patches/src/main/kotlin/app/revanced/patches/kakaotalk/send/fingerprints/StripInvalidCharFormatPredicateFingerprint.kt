package app.revanced.patches.kakaotalk.send.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

private const val INPUT_FILTER_TYPE = "Landroid/text/InputFilter;"

// The input filter itself has the signature every input filter has, so it is matched through
// the per char predicate that delegates to KakaoTalk's invalid char format check.
internal object StripInvalidCharFormatPredicateFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf("C"),
    filters = listOf(
        methodCall(parameters = listOf("C", "[C"), returnType = "Z"),
    ),
    custom = { _, classDef -> INPUT_FILTER_TYPE in classDef.interfaces },
)