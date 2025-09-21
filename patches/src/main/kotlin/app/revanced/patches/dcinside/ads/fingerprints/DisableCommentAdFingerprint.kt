package app.revanced.patches.dcinside.ads.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal val postReadReplyAdViewFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR)
    parameters("Landroid/content/Context;", "Landroid/util/AttributeSet;", "I")
    returns("V")
    custom { method, classDef -> classDef.toString() == "Lcom/dcinside/app/view/PostReadReplyAdView;" }
}