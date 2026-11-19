package app.revanced.patches.kakaotalk.misc.openlink.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal object DefaultExternalBrowserFingerprint : Fingerprint(
    parameters = emptyList(),
    returnType = "L",
    filters = listOf(
        string("defaultExternalBrowser"),
        opcode(Opcode.CONST_4),
    ),
    custom = { _, classDef -> classDef.sourceFile == "CbtPreferences.kt" },
)