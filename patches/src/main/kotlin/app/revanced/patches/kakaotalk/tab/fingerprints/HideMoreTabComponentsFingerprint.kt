package app.revanced.patches.kakaotalk.tab.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.newInstance
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

private val LIST_ADD_METHOD_CALL = methodCall(
    opcode = Opcode.INVOKE_INTERFACE,
    smali = "Ljava/util/List;->add(Ljava/lang/Object;)Z",
)

internal object AddMoreTabBodySectionsFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "Ljava/util/List;",
    strings = listOf("2604_vari_A_app"),
    custom = { method, classDef ->
        classDef.sourceFile == "MoreTabViewModel.kt" &&
                method.parameterTypes.size == 5
    }
)

internal object AddMoreTabServiceSectionsFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "V",
    strings = listOf(
        "2604_vari_A_app",
        "2604_vari_B_cat",
        "2604_vari_C_ad",
    ),
    custom = { _, classDef ->
        classDef.sourceFile == "MoreTabViewModel.kt"
    }
)

internal object MoreTabGamePlaySectionFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    strings = listOf("GamePlay(uiModel="),
    custom = { method, classDef ->
        classDef.sourceFile == "MoreTabItem.kt" &&
                method.parameterTypes.isEmpty()
    }
)

internal object MoreTabKakaoNowSectionFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    strings = listOf("KakaoNow(uiModel="),
    custom = { method, classDef ->
        classDef.sourceFile == "MoreTabItem.kt" &&
                method.parameterTypes.isEmpty()
    }
)

internal object MoreTabKakaoPaySectionFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    strings = listOf("KakaoPay(uiModel="),
    custom = { method, classDef ->
        classDef.sourceFile == "MoreTabItem.kt" &&
                method.parameterTypes.isEmpty()
    }
)

internal object MoreTabWeatherSectionFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    strings = listOf("Weather(uiModel="),
    custom = { method, classDef ->
        classDef.sourceFile == "MoreTabItem.kt" &&
                method.parameterTypes.isEmpty()
    }
)

internal object WeatherViewHolderBindFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    strings = listOf("item"),
    custom = { method, classDef ->
        classDef.sourceFile == "WeatherViewHolder.kt" &&
                !classDef.type.contains("$") &&
                method.name != "<init>" &&
                method.accessFlags and AccessFlags.BRIDGE.value == 0 &&
                method.accessFlags and AccessFlags.SYNTHETIC.value == 0 &&
                method.parameterTypes.size == 1
    }
)

internal object MoreTabServiceGroupSectionFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    strings = listOf("ServiceGroup(uiModel="),
    custom = { method, classDef ->
        classDef.sourceFile == "MoreTabItem.kt" &&
                method.parameterTypes.isEmpty()
    }
)

internal object MoreTabGlobalServiceGroupSectionFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    strings = listOf("GlobalServiceGroup(uiModel="),
    custom = { method, classDef ->
        classDef.sourceFile == "MoreTabItem.kt" &&
                method.parameterTypes.isEmpty()
    }
)

internal fun moreTabGlobalServiceGroupAdditionFingerprint(globalServiceGroupType: String) = Fingerprint(
    name = "invokeSuspend",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/Object;"),
    filters = listOf(
        newInstance("Ljava/util/ArrayList;"),
        newInstance(globalServiceGroupType),
        LIST_ADD_METHOD_CALL,
    ),
)

internal fun moreTabServiceGroupViewHolderBindFingerprint(itemType: String) =
    moreTabItemViewHolderBindFingerprint("ServiceGroupPagerViewHolder.kt", itemType)

internal fun moreTabGlobalServiceGroupViewHolderBindFingerprint(itemType: String) =
    moreTabItemViewHolderBindFingerprint("GlobalServiceGroupViewHolder.kt", itemType)

internal fun moreTabLineServiceViewHolderBindFingerprint(itemType: String) =
    moreTabItemViewHolderBindFingerprint("LineServiceViewHolder.kt", itemType)

private fun moreTabItemViewHolderBindFingerprint(
    sourceFile: String,
    itemType: String,
) = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf(itemType),
    custom = { method, classDef ->
        classDef.sourceFile == sourceFile &&
                method.accessFlags and AccessFlags.BRIDGE.value == 0 &&
                method.accessFlags and AccessFlags.SYNTHETIC.value == 0
    },
)

internal object MoreTabLineServiceSectionFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    strings = listOf("LineService(uiModel="),
    custom = { method, classDef ->
        classDef.sourceFile == "MoreTabItem.kt" &&
                method.parameterTypes.isEmpty()
    }
)
