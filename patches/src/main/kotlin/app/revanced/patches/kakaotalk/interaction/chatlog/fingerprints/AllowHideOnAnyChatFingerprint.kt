package app.revanced.patches.kakaotalk.interaction.chatlog.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

internal object CheckIsAllowedHideFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(
        "Lcom/kakao/talk/module/openlink/contract/OpenLinkModuleFacade;",
        "J",
        "L",
        "L",
    ),
    returnType = "Z",
    filters = listOf(
        methodCall(returnType = "Lcom/kakao/talk/openlink/db/model/OpenLink;"),
        methodCall(returnType = "Lcom/kakao/talk/openlink/db/model/OpenLinkProfile;"),
    ),
    custom = { _, classDef -> classDef.sourceFile == "DeletableAction.kt" }
)
