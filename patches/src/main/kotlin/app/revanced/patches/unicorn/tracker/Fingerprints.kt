package app.revanced.patches.unicorn.tracker

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object DisableSentryFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    strings = listOf(
        "Fatal error during SentryAndroid.init(...)",
        "Failed to initialize Sentry\'s SDK"
    ),
    custom = { method, classDef -> classDef.sourceFile == "SentryAndroid.java" }
)