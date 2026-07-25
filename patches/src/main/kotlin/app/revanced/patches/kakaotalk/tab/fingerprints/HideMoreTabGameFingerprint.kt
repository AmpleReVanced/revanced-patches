package app.revanced.patches.kakaotalk.tab.fingerprints

import app.morphe.patcher.Fingerprint

internal object MoreTabPagerGameItemFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    strings = listOf("Game"),
    custom = { method, classDef ->
        classDef.sourceFile == "MoreTabPagerItem.kt" &&
                method.parameterTypes.isEmpty()
    }
)

internal object MoreTabPagerHomeItemFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    strings = listOf("Home"),
    custom = { method, classDef ->
        classDef.sourceFile == "MoreTabPagerItem.kt" &&
                method.parameterTypes.isEmpty()
    }
)
