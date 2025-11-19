package app.revanced.patches.dcinside.ads.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal val postReadCommentAdViewFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR)
    parameters("Landroid/content/Context;", "Landroid/util/AttributeSet;", "I")
    returns("V")
    custom { method, classDef -> classDef.type == "Lcom/dcinside/app/view/PostReadCommentAdView;" }
}

internal val postReadCommentTopAdViewFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR)
    parameters("Landroid/content/Context;", "Landroid/util/AttributeSet;", "I")
    returns("V")
    custom { method, classDef -> classDef.type == "Lcom/dcinside/app/view/PostReadCommentTopAdView;" }
}