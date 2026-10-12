package app.revanced.patches.kakaotalk.layout.tab.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

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

internal object NowTabPagerAdapterFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = listOf("I"),
    returnType = "Landroidx/fragment/app/Fragment;",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.SGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.IF_NE,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_INTERFACE,
    ),
    custom = { method, classDef ->
        classDef.sourceFile == "NowTabPagerAdapter.kt"
    }
)

internal object GetOpenLinkModuleFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    parameters = listOf(),
    returnType = "Lcom/kakao/talk/module/openlink/contract/OpenLinkModuleFacade;",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.SGET_OBJECT,
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CHECK_CAST,
        Opcode.RETURN_OBJECT,
    ),
    custom = { method, classDef -> classDef.sourceFile == "ModuleFacades.kt" }
)

internal object NowChildTabFromPositionFingerprint : Fingerprint(
    parameters = listOf("Ljava/lang/Integer;"),
    custom = { method, classDef ->
        classDef.sourceFile == "NowChildTab.kt" &&
                classDef.type.contains("$") &&
                method.returnType == classDef.type.substringBefore("$") + ";"
    }
)

internal object NowChildTabFromNameFingerprint : Fingerprint(
    parameters = listOf("Ljava/lang/String;"),
    strings = listOf("brand", "openlink", "openchat", "shortform"),
    custom = { method, classDef ->
        classDef.sourceFile == "NowChildTab.kt" &&
                classDef.type.contains("$") &&
                method.returnType == classDef.type.substringBefore("$") + ";"
    }
)

internal object ChooseOpenLinkTabFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L"),
    filters = listOf(
        methodCall(name = "getPosition", parameters = listOf(), returnType = "I"),
        methodCall("Landroidx/viewpager2/widget/ViewPager2;->setCurrentItem(IZ)V"),
    ),
    custom = { _, classDef -> classDef.sourceFile == "NowFragment.kt" },
)

internal object ChooseNowChildTabFingerprint : Fingerprint(
    name = "invokeSuspend",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/Object;",
    strings = listOf(
        "call to 'resume' before 'invoke' with coroutine",
        "NOW_TAB",
        "binding",
    ),
    filters = listOf(
        methodCall(
            name = "getPosition",
            parameters = listOf(),
            returnType = "I",
            opcode = Opcode.INVOKE_VIRTUAL,
        ),
    ),
    custom = { method, classDef -> classDef.sourceFile == "NowFragment.kt" }
)

internal object NowTabReselectionFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(),
    returnType = "V",
    filters = listOf(
        methodCall(name = "getPosition", parameters = listOf(), returnType = "I"),
        methodCall("Landroidx/viewpager2/widget/ViewPager2;->setCurrentItem(IZ)V"),
    ),
    custom = { _, classDef -> classDef.sourceFile == "NowFragment.kt" },
)

internal fun nowTabPageSelectionFingerprint(tabType: String) = Fingerprint(
    filters = listOf(
        methodCall(definingClass = tabType, name = "getPosition", parameters = listOf(), returnType = "I"),
        methodCall("Landroidx/viewpager2/widget/ViewPager2;->setCurrentItem(IZ)V"),
    ),
    custom = { _, classDef -> classDef.sourceFile == "NowFragment.kt" },
)

internal fun nowChildTabSetterFingerprint(tabType: String) = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(tabType),
    returnType = "V",
    filters = listOf(
        methodCall(name = "setValue", parameters = listOf("Ljava/lang/Object;"), returnType = "V"),
    ),
    custom = { _, classDef -> classDef.sourceFile == "MainTabConfig.kt" },
)

internal fun nowChildTabObserverFingerprint(setter: MethodReference) = Fingerprint(
    parameters = listOf(setter.parameterTypes.single().toString(), "Lkotlin/coroutines/Continuation;"),
    returnType = "Ljava/lang/Object;",
    filters = listOf(
        methodCall(
            definingClass = setter.definingClass,
            name = setter.name,
            parameters = setter.parameterTypes.map(CharSequence::toString),
            returnType = setter.returnType,
        ),
    ),
    custom = { _, classDef -> classDef.sourceFile == "NowFragment.kt" },
)