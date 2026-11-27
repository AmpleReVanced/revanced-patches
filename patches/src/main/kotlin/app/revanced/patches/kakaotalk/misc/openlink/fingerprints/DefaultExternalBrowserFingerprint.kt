package app.revanced.patches.kakaotalk.misc.openlink.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
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

internal object BrowserIntentFingerprint : Fingerprint(
    definingClass = "Lcom/kakao/talk/util/IntentUtils;",
    parameters = listOf("Landroid/content/Context;", "Ljava/lang/String;", "Z"),
    returnType = "Landroid/content/Intent;",
    filters = listOf(
        methodCall(
            definingClass = "Lcom/kakao/talk/util/IntentUtils;",
            parameters = emptyList(),
            returnType = "Z",
        ),
        methodCall(definingClass = "Lcom/kakao/talk/widget/webview/WebViewHelper;", name = "getWebBrowserAction"),
    ),
)
