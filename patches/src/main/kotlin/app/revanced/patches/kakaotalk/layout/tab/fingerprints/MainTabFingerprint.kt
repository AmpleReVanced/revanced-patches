package app.revanced.patches.kakaotalk.layout.tab.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

private const val TAB_INFO_CLASS = "Lcom/kakao/talk/activity/main/MainTabPagerAdapter\$TabInfo;"

internal fun mainTabFingerprint(tabName: String) = Fingerprint(
    returnType = TAB_INFO_CLASS,
    parameters = listOf("L"),
    filters = listOf(
        fieldAccess(opcode = Opcode.SGET_OBJECT, definingClass = TAB_INFO_CLASS, name = tabName),
        opcode(Opcode.RETURN_OBJECT, MatchAfterImmediately()),
    ),
    custom = { _, classDef -> classDef.sourceFile == "MainTabConfig.kt" },
)
