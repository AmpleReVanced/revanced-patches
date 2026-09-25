package app.revanced.patches.starnote.premium

import app.revanced.patches.starnote.shared.MethodCallFingerprint
import app.revanced.patches.starnote.shared.ProtectedDexFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

private fun premiumStateFingerprint(id: String, name: String) = ProtectedDexFingerprint(
    id = id,
    name = name,
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = listOf(),
    returnType = "Z",
    methodCalls = listOf(
        MethodCallFingerprint(
            parameters = listOf("I", "Ljava/lang/Object;", "[Ljava/lang/Object;"),
            returnType = "Z",
        ),
    ),
)

internal val PermanentPremiumFingerprint = premiumStateFingerprint("permanent premium state", "isVIPPermanent")

internal val PremiumRoleFingerprint = premiumStateFingerprint("premium role state", "isVipRole")

internal val NoteCreateQuotaFingerprint = ProtectedDexFingerprint(
    id = "local note creation quota",
    name = "getNoteCreateQuota",
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = listOf("Z"),
    returnType = "J",
    strings = listOf("notebook", "normalNotebook"),
)