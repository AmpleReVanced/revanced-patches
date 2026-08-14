package app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object CheckIsAllowedHideFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("L", "L"),
    returnType = "Z",
    filters = listOf(
        methodCall(returnType = "Lcom/kakao/talk/openlink/db/model/OpenLink;"),
        methodCall(returnType = "Lcom/kakao/talk/openlink/db/model/OpenLinkProfile;"),
    ),
    custom = { _, classDef -> classDef.sourceFile == "DeletableAction.kt" }
)

internal object CheckIsEqualWithMyUserIdFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = listOf("J"),
    returnType = "Z",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_WIDE,
        Opcode.CMP_LONG,
        Opcode.IF_NEZ,
        Opcode.CONST_4,
        Opcode.RETURN,
        Opcode.CONST_4,
        Opcode.RETURN
    ),
    custom = { _, classDef -> classDef.sourceFile == "Me.kt" }
)
