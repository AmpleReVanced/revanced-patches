package app.revanced.patches.starnote.premium

import app.revanced.patches.starnote.shared.MethodCallFingerprint
import app.revanced.patches.starnote.shared.ProtectedDexFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

private fun premiumStateFingerprint(id: String, name: String) = ProtectedDexFingerprint(
    id = id,
    definingClass = "Lcom/onyx/android/sdk/data/account/AccountDataBundle;",
    name = name,
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = listOf(),
    returnType = "Z",
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

internal val RemainingNoteQuotaFingerprint = ProtectedDexFingerprint(
    id = "remaining local note quota",
    definingClass = "Lcom/onyx/galaxy/note/category/viewmodel/NoteQuotaViewModel;",
    name = "getLeftNoteQuota",
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = listOf(),
    returnType = "J",
    methodCalls = listOf(
        MethodCallFingerprint(
            name = "getNoteCreateQuota\$default",
            returnType = "J",
        ),
        MethodCallFingerprint(
            name = "getConsumedNoteQuota",
            parameters = listOf(),
            returnType = "I",
        ),
    ),
)

internal val NoteRegistrationGuideFingerprint = ProtectedDexFingerprint(
    id = "note creation registration guide",
    definingClass = "Lcom/onyx/galaxy/note/category/viewmodel/NoteQuotaViewModel;",
    name = "checkGuideRegister",
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = listOf("Landroid/content/Context;"),
    returnType = "Z",
    methodCalls = listOf(
        MethodCallFingerprint(
            name = "isAlreadyLogin",
            parameters = listOf(),
            returnType = "Z",
        ),
        MethodCallFingerprint(
            name = "getRegisterGuideQuota",
            parameters = listOf(),
            returnType = "J",
        ),
    ),
)
