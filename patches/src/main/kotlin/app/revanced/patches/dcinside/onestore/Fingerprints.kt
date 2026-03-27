package app.revanced.patches.dcinside.onestore

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object ApplicationConfigClassFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = listOf("Landroid/content/Context;"),
    returnType = "V",
    strings = listOf("android.settings.APPLICATION_DETAILS_SETTINGS", "android.intent.category.DEFAULT", "package:"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.IF_NEZ,
        Opcode.RETURN_VOID,
        Opcode.NEW_INSTANCE,
        Opcode.INVOKE_DIRECT,
    )
)