package app.revanced.patches.kakaotalk.layout.tab.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.StringComparisonType
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags

internal object MainNavigationSuiteFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(
        "L",
        "Ljava/util/List;",
        "Lkotlin/jvm/functions/Function1;",
        "I",
        "Lkotlin/jvm/functions/Function1;",
        "Lkotlin/jvm/functions/Function1;",
        "L",
        "Z",
        "Z",
        "Z",
        "L",
        "I",
        "I",
    ),
    filters = listOf(
        string(
            "com.kakao.talk.activity.adaptive.main.MainNavigationSuite (",
            StringComparisonType.STARTS_WITH,
        ),
    ),
    custom = { _, classDef -> classDef.sourceFile == "MainNavigation.kt" },
)