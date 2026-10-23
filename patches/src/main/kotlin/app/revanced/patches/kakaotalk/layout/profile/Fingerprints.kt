package app.revanced.patches.kakaotalk.layout.profile

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal const val PROFILE_EDIT_BUTTON_LABEL = "button_for_profile_edit"

internal object ProfileMediaViewerScreenFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L", "Z", "Ljava/lang/String;", "Ljava/lang/String;", "L", "L", "L", "I"),
    filters = listOf(
        fieldAccess(name = PROFILE_EDIT_BUTTON_LABEL, type = "I", opcode = Opcode.SGET),
    ),
    custom = { _, classDef -> classDef.sourceFile == "ProfileMediaViewerScreen.kt" },
)

internal object ProfileItemDetailOnCreateFingerprint : Fingerprint(
    name = "onCreate",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    custom = { _, classDef -> classDef.sourceFile == "ProfileItemDetailActivity.kt" },
)