package app.revanced.patches.kakaotalk.misc.debugging

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.classDefForEach
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.util.cloneMutable
import app.revanced.patches.kakaotalk.misc.debugging.fingerprints.DefaultWebViewClientFingerprint
import app.revanced.patches.kakaotalk.misc.debugging.fingerprints.ProxyHostnameVerifierFingerprint
import app.revanced.patches.kakaotalk.misc.debugging.fingerprints.WEB_VIEW_CLIENT
import app.revanced.patches.kakaotalk.misc.debugging.fingerprints.WEB_VIEW_SSL_PARAMETERS
import app.revanced.patches.kakaotalk.misc.debugging.fingerprints.WebViewSslErrorFingerprint
import app.revanced.util.smaliReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val HANDLE_SSL_ERROR =
    "$SSL_DEBUG_EXTENSION_CLASS->filterWebViewSslError(Landroid/webkit/SslErrorHandler;Landroid/net/http/SslError;)Landroid/webkit/SslErrorHandler;"
private const val PROXY_WEB_VIEW_CLIENT =
    "Lapp/revanced/extension/kakaotalk/patches/DisableSslPinningPatch\$ProxyWebViewClient;"

internal fun allowWebViewProxyCertificates() {
    ProxyHostnameVerifierFingerprint.apply {
        val verify = originalMethod.instructions.mapNotNull { it.getReference<MethodReference>() }.single {
            it.definingClass == originalClassDef.type && it.parameterTypes ==
                listOf("Ljava/lang/String;", "Ljava/security/cert/X509Certificate;") && it.returnType == "Z"
        }
        val instance = originalClassDef.staticFields.single { it.type == originalClassDef.type }
        val extension = mutableClassDefBy(SSL_DEBUG_EXTENSION_CLASS)
        val stub = extension.methods.single { it.name == "verifyHostname" }
        extension.methods.remove(stub)
        extension.methods.add(stub.cloneMutable(additionalRegisters = 1).apply {
            addInstructions(
                0,
                """
                    sget-object v0, ${instance.smaliReference}
                    invoke-virtual {v0, p0, p1}, $verify
                    move-result v0
                    return v0
                """,
            )
        })
    }

    WebViewSslErrorFingerprint.matchAll().forEach { match ->
        match.method.apply {
            addInstructionsWithLabels(
                0,
                """
                    invoke-static/range {p2 .. p3}, $HANDLE_SSL_ERROR
                    move-result-object p2
                    if-nez p2, :original
                    return-void
                """,
                ExternalLabel("original", getInstruction(0)),
            )
        }
    }

    classDefForEach { classDef ->
        if (classDef.superclass != WEB_VIEW_CLIENT || classDef.type.startsWith("Lapp/revanced/extension/") ||
            classDef.methods.any { it.name == "onReceivedSslError" && it.parameterTypes == WEB_VIEW_SSL_PARAMETERS }
        ) return@classDefForEach
        mutableClassDefBy(classDef.type).methods.add(
            ImmutableMethod(
                classDef.type,
                "onReceivedSslError",
                WEB_VIEW_SSL_PARAMETERS.map { ImmutableMethodParameter(it, null, null) },
                "V",
                AccessFlags.PUBLIC.value,
                null,
                null,
                MutableMethodImplementation(4),
            ).toMutable().apply {
                addInstructionsWithLabels(
                    0,
                    """
                        invoke-static/range {p2 .. p3}, $HANDLE_SSL_ERROR
                        move-result-object p2
                        if-eqz p2, :handled
                        invoke-super {p0, p1, p2, p3}, $WEB_VIEW_CLIENT->onReceivedSslError(${WEB_VIEW_SSL_PARAMETERS.joinToString("")})V
                        :handled
                        return-void
                    """,
                )
            },
        )
    }

    DefaultWebViewClientFingerprint.matchAll().forEach { match ->
        match.method.apply {
            val original = instructions.toList()
            original.forEachIndexed { index, instruction ->
                if (instruction.opcode != Opcode.NEW_INSTANCE ||
                    instruction.getReference<TypeReference>()?.type != WEB_VIEW_CLIENT
                ) return@forEachIndexed
                val register = (instruction as OneRegisterInstruction).registerA
                val constructor = original.indices.first { callIndex ->
                    if (callIndex <= index || original[callIndex].getReference<MethodReference>()?.toString() !=
                        "$WEB_VIEW_CLIENT-><init>()V"
                    ) return@first false
                    when (val call = original[callIndex]) {
                        is FiveRegisterInstruction -> call.registerC == register
                        is RegisterRangeInstruction -> call.startRegister == register
                        else -> false
                    }
                }
                replaceInstruction(index, "new-instance v$register, $PROXY_WEB_VIEW_CLIENT")
                replaceInstruction(constructor, "invoke-direct/range {v$register .. v$register}, $PROXY_WEB_VIEW_CLIENT-><init>()V")
            }
        }
    }
}
