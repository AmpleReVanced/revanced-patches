package app.revanced.patches.starnote.login

import app.revanced.patches.starnote.shared.MethodCallFingerprint
import app.revanced.patches.starnote.shared.ProtectedDexFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal val LoginRequiredFingerprint = ProtectedDexFingerprint(
    id = "login requirement",
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = listOf(),
    returnType = "Z",
    methodCalls = listOf(
        MethodCallFingerprint(
            definingClass = "Landroidx/lifecycle/LiveData;",
            parameters = listOf(),
            returnType = "Ljava/lang/Object;",
        ),
        MethodCallFingerprint(
            definingClass = "Ljava/util/List;",
            parameters = listOf(),
            returnType = "I",
        ),
        MethodCallFingerprint(
            definingClass = "Ljava/util/NoSuchElementException;",
            parameters = listOf(),
            returnType = "V",
        ),
    ),
)