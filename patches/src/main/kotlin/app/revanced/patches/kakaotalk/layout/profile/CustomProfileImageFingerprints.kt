package app.revanced.patches.kakaotalk.layout.profile

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

internal object CustomProfileCreateFingerprint : Fingerprint(
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    custom = { _, classDef -> classDef.sourceFile == "CustomProfileCreateActivity.kt" },
)

internal object CustomProfileMenuFingerprint : Fingerprint(
    classFingerprint = CustomProfileCreateFingerprint,
    name = "onCreateOptionsMenu",
    returnType = "Z",
    parameters = listOf("Landroid/view/Menu;"),
)

internal object CustomProfileBitmapFingerprint : Fingerprint(
    classFingerprint = CustomProfileCreateFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Landroid/graphics/Bitmap;",
    parameters = listOf("Landroid/view/View;"),
    filters = listOf(methodCall("Landroid/view/View;->draw(Landroid/graphics/Canvas;)V")),
)

internal object CustomProfileColorFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("I", "I", "I", "I"),
    filters = listOf(methodCall("Landroid/view/View;->setBackgroundColor(I)V")),
    custom = { _, classDef -> classDef.sourceFile == "CustomProfileCreateActivity.kt" },
)