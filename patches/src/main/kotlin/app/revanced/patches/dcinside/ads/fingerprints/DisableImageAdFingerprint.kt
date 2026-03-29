package app.revanced.patches.dcinside.ads.fingerprints

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object PostReadImageAdViewFingerprint : Fingerprint(
    definingClass = "Lcom/dcinside/app/view/PostReadImageAdView;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    parameters = listOf("Landroid/content/Context;", "Landroid/util/AttributeSet;", "I"),
    returnType = "V"
)

internal object RefreshImageAdFingerprint : Fingerprint(
    definingClass = "Lcom/dcinside/app/view/PostReadImageAdView;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    strings = listOf("galleryGrade")
)