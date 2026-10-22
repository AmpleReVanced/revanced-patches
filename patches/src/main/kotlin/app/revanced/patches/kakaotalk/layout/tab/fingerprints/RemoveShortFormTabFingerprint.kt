package app.revanced.patches.kakaotalk.layout.tab.fingerprints

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object NowChildTabFingerprint : Fingerprint(
    name = "<clinit>",
    superclass = "Ljava/lang/Enum;",
    strings = listOf("ShortForm", "Openlink", "Brand", "None"),
    custom = { _, classDef -> classDef.sourceFile == "NowChildTab.kt" },
)

internal object NowChildTabFromPositionFingerprint : Fingerprint(
    parameters = listOf("Ljava/lang/Integer;"),
    returnType = "L",
    custom = { _, classDef -> classDef.sourceFile == "NowChildTab.kt" },
)

internal object NowChildTabFromNameFingerprint : Fingerprint(
    parameters = listOf("Ljava/lang/String;"),
    returnType = "L",
    strings = listOf("brand", "openlink", "openchat", "shortform"),
    custom = { _, classDef -> classDef.sourceFile == "NowChildTab.kt" },
)

private val TAB_SELECTION_SOURCES = setOf(
    "NowScreenViewModel.kt",
    "NowTabLandingResolver.kt",
    "NowSubTabPreferenceStore.kt",
    "NowTabViewModel.kt",
    "MainTabConfig.kt",
)

internal fun nowChildTabSelectionFingerprint(tabType: String) = Fingerprint(
    custom = { method, classDef ->
        method.implementation != null && tabType in method.parameterTypes &&
                classDef.sourceFile in TAB_SELECTION_SOURCES
    },
)

internal object NowTabChipStateFingerprint : Fingerprint(
    name = "toString",
    strings = listOf("NowTabChipState(tab="),
    custom = { _, classDef -> classDef.sourceFile == "NowTabChip.kt" },
)

internal fun nowTabChipFingerprint(stateType: String) = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(
        stateType,
        "Lkotlin/jvm/functions/Function1;",
        "Lkotlin/jvm/functions/Function1;",
        "Lkotlin/jvm/functions/Function1;",
        "L", "L", "L", "I", "I",
    ),
    custom = { _, classDef -> classDef.sourceFile == "NowTabChip.kt" },
)

internal object NowBrandChipFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(
        "L", "Lcom/kakao/adfit/ads/brandchip/BrandChipAd;", "L",
        "Lkotlin/jvm/functions/Function1;",
        "Lkotlin/jvm/functions/Function1;",
        "Lkotlin/jvm/functions/Function1;",
        "L", "L", "I", "I",
    ),
    custom = { _, classDef -> classDef.sourceFile == "NowTabChipRow.kt" },
)
