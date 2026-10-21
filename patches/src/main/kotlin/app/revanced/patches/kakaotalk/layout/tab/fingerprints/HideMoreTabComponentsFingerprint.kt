package app.revanced.patches.kakaotalk.layout.tab.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object MoreTabListAdapterFingerprint : Fingerprint(
    name = "getItemViewType",
    parameters = listOf("I"),
    returnType = "I",
    custom = { _, classDef -> classDef.sourceFile == "MoreTabListAdapter.kt" },
)

internal object MoreTabKakaoPayExperimentFingerprint : Fingerprint(
    name = "toString",
    strings = listOf("KakaoPayExperiment"),
    custom = { _, classDef -> classDef.sourceFile == "MoreTabItem.kt" },
)

private val LIST_ADD_METHOD_CALL = methodCall(
    opcode = Opcode.INVOKE_INTERFACE,
    smali = "Ljava/util/List;->add(Ljava/lang/Object;)Z",
)

internal object AddMoreTabServiceSectionsFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Ljava/util/List;", "L", "L", "L"),
    filters = listOf(LIST_ADD_METHOD_CALL),
    custom = { _, classDef ->
        classDef.sourceFile == "MoreTabViewModel.kt"
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

internal object MoreTabLineServiceSectionFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    strings = listOf("LineService(uiModel="),
    custom = { method, classDef ->
        classDef.sourceFile == "MoreTabItem.kt" &&
                method.parameterTypes.isEmpty()
    }
)

internal object MoreTabLineGridServiceSectionFingerprint : Fingerprint(
    name = "toString",
    strings = listOf("LineGridService(uiModel="),
    custom = { _, classDef -> classDef.sourceFile == "MoreTabItem.kt" },
)
