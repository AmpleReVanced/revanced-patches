package app.revanced.patches.kakaotalk.misc.integrity.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

internal object InstallSourceFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Landroid/content/Context;"),
    returnType = "Ljava/lang/String;",
    filters = listOf(
        methodCall("Landroid/content/pm/PackageManager;->getInstallerPackageName(Ljava/lang/String;)Ljava/lang/String;"),
    ),
    custom = { _, classDef -> classDef.sourceFile == "PlatformUtils.kt" },
)