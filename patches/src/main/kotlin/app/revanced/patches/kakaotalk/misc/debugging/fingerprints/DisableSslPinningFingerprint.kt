package app.revanced.patches.kakaotalk.misc.debugging.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.classDefByOrNull
import app.morphe.patcher.util.getReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

internal const val WEB_VIEW_CLIENT = "Landroid/webkit/WebViewClient;"
internal val WEB_VIEW_SSL_PARAMETERS = listOf(
    "Landroid/webkit/WebView;",
    "Landroid/webkit/SslErrorHandler;",
    "Landroid/net/http/SslError;",
)

internal object SslDebugApplicationContextFingerprint : Fingerprint(
    definingClass = "Lcom/kakao/talk/application/App;",
    name = "attachBaseContext",
    parameters = listOf("Landroid/content/Context;"),
    returnType = "V",
)

internal object CertificatePinCheckFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Ljava/lang/String;", "Lkotlin/jvm/functions/Function0;"),
    returnType = "V",
    strings = listOf("Certificate pinning failure!\n  Peer certificate chain:"),
    custom = { _, classDef -> classDef.sourceFile == "CertificatePinner.kt" },
)

internal object DefaultTrustManagerFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = emptyList(),
    returnType = "Ljavax/net/ssl/X509TrustManager;",
    strings = listOf("Unexpected default trust managers: "),
    custom = { _, classDef -> classDef.sourceFile == "Platform.kt" },
)

internal object ProxyHostnameVerifierFingerprint : Fingerprint(
    name = "verify",
    parameters = listOf("Ljava/lang/String;", "Ljavax/net/ssl/SSLSession;"),
    returnType = "Z",
    custom = { _, classDef ->
        classDef.sourceFile == "OkHostnameVerifier.kt" && "Ljavax/net/ssl/HostnameVerifier;" in classDef.interfaces
    },
)

internal object WebViewSslErrorFingerprint : Fingerprint(
    name = "onReceivedSslError",
    parameters = WEB_VIEW_SSL_PARAMETERS,
    returnType = "V",
    custom = { _, classDef ->
        !classDef.type.startsWith("Lapp/revanced/extension/") &&
            generateSequence(classDef.superclass) { classDefByOrNull(it)?.superclass }.any { it == WEB_VIEW_CLIENT }
    },
)

internal object DefaultWebViewClientFingerprint : Fingerprint(
    filters = listOf(methodCall("$WEB_VIEW_CLIENT-><init>()V")),
    custom = { method, _ ->
        method.implementation?.instructions?.any {
            it.opcode == Opcode.NEW_INSTANCE && it.getReference<TypeReference>()?.type == WEB_VIEW_CLIENT
        } == true
    },
)
