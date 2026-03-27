package app.revanced.patches.dcinside.ads.fingerprints

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object PostReadCommentAdViewFingerprint : Fingerprint(
    definingClass = "Lcom/dcinside/app/view/PostReadCommentAdView;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    parameters = listOf("Landroid/content/Context;", "Landroid/util/AttributeSet;", "I"),
    returnType = "V",
)

internal object PostReadCommentTopAdViewFingerprint : Fingerprint(
    definingClass = "Lcom/dcinside/app/view/PostReadCommentTopAdView;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    parameters = listOf("Landroid/content/Context;", "Landroid/util/AttributeSet;", "I"),
    returnType = "V"
)