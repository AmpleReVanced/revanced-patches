package app.revanced.patches.kakaotalk.misc.feature

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

internal object GetFeatureFlagValueFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    parameters = listOf("L", "Ljava/util/Set;", "Z", "I"),
    returnType = "Z",
    filters = listOf(methodCall("Ljava/util/Set;->contains(Ljava/lang/Object;)Z")),
    custom = { _, classDef -> classDef.sourceFile == "FeatureRuntime.kt" },
)

internal object HashFeatureKeyFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Ljava/lang/String;", "I"),
    returnType = "Ljava/lang/String;",
    strings = listOf("SHA-256"),
    custom = { _, classDef -> classDef.sourceFile == "FeatureKeyHasher.kt" },
)

internal object HashFeatureKeyInExtensionFingerprint : Fingerprint(
    definingClass = "Lapp/revanced/extension/kakaotalk/feature/Flag;",
    name = "hashFeatureKey",
)

internal object GetFeatureFlagsInExtensionFingerprint : Fingerprint(
    definingClass = "Lapp/revanced/extension/kakaotalk/feature/Flag;",
    name = "getFeatureFlags",
)