package app.revanced.patches.kakaotalk.tracker.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal val disableSentryFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.DECLARED_SYNCHRONIZED)
    returns("V")
    strings("Fatal error during SentryAndroid.init(...)", "Failed to initialize Sentry\'s SDK")
    custom { method, classDef -> classDef.sourceFile == "SentryAndroid.java" }
}