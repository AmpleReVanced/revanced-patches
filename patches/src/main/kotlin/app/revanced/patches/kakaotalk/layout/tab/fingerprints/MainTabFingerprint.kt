package app.revanced.patches.kakaotalk.layout.tab.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

internal object MainTabFingerprint : Fingerprint(
    name = "<clinit>",
    strings = listOf("FolderChat", "GlobalMore"),
    custom = { _, classDef ->
        classDef.sourceFile == "MainTab.kt" && classDef.superclass == "Ljava/lang/Enum;"
    },
)

internal fun mainTabFingerprint(tabType: String, tabName: String) = Fingerprint(
    returnType = tabType,
    parameters = listOf("L"),
    filters = listOf(
        fieldAccess(opcode = Opcode.SGET_OBJECT, definingClass = tabType, name = tabName),
        opcode(Opcode.RETURN_OBJECT, MatchAfterImmediately()),
    ),
    custom = { _, classDef -> classDef.sourceFile == "MainTabConfig.kt" },
)
