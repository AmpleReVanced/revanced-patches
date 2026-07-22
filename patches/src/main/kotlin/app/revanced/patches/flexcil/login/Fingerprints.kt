package app.revanced.patches.flexcil.login

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object ShowAccountLoginFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = listOf("Lcom/flexcil/flexcilnote/derivedproduct/flexcilaccount/ui/FlexcilAccountActivity;"),
    returnType = "V",
    custom = { _, classDef -> classDef.sourceFile == "FlexcilAccountActivity.kt" },
)