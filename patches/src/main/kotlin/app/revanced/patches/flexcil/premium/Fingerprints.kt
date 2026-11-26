package app.revanced.patches.flexcil.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

internal object PremiumProductsGateFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = listOf(),
    returnType = "Z",
    strings = listOf("productIdentifiers"),
    custom = { _, classDef -> classDef.sourceFile == "FlexcilProducts.kt" },
)

internal object AccountPremiumGateFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = listOf(),
    returnType = "Z",
    strings = listOf("productIdentifiers"),
    custom = { _, classDef -> classDef.sourceFile == "FlexcilAccountClient.kt" },
)

internal object ActiveSubscriptionFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = listOf(),
    returnType = "Z",
    filters = listOf(
        methodCall("Landroid/os/SystemClock;->elapsedRealtime()J"),
        methodCall("Ljava/lang/System;->currentTimeMillis()J"),
    ),
    custom = { _, classDef -> classDef.sourceFile == "FlexcilAccountClient.kt" },
)

internal object B2bLicenseFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = listOf(),
    returnType = "Z",
    strings = listOf("flexcilnote.premium.b2bc"),
    custom = { _, classDef -> classDef.sourceFile == "FlexcilAccountClient.kt" },
)

internal object OwnsQueriedProductFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = listOf("Ljava/util/List;"),
    returnType = "Z",
    strings = listOf("productIdentifiers"),
    custom = { _, classDef -> classDef.sourceFile == "ProductDetailsHelper.kt" },
)
