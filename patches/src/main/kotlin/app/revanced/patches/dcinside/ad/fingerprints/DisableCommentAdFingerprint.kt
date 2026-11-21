package app.revanced.patches.dcinside.ad.fingerprints

import app.morphe.patcher.Fingerprint

internal fun commentAdConstructorFingerprint(position: String) = Fingerprint(
    definingClass = "Lcom/dcinside/app/view/PostReadComment${position}AdView;",
    name = "<init>",
    parameters = listOf("Landroid/content/Context;", "Landroid/util/AttributeSet;", "I"),
    returnType = "V",
)
