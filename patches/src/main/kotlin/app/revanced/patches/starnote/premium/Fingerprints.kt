package app.revanced.patches.starnote.premium

import app.revanced.patches.starnote.shared.MethodCallFingerprint
import app.revanced.patches.starnote.shared.ProtectedDexFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal val PermanentPremiumFingerprint = ProtectedDexFingerprint(
    id = "permanent premium state",
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = listOf(),
    returnType = "Z",
    strings = listOf("galaxyStarNotePermanentMember"),
)

internal val PremiumRoleFingerprint = ProtectedDexFingerprint(
    id = "premium role state",
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = listOf(),
    returnType = "Z",
    methodCalls = listOf(
        MethodCallFingerprint(
            parameters = listOf(
                "Ljava/lang/Iterable;",
                "Lkotlin/jvm/functions/Function1;",
            ),
            returnType = "Z",
        ),
    ),
)