package app.revanced.patches.kakaotalk.misc.settings

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.revanced.util.parameterTypeNames
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object MainSettingItemTypeFingerprint : Fingerprint(
    superclass = "Ljava/lang/Enum;",
    name = "<clinit>",
    strings = listOf("LABORATORY"),
    custom = { _, classDef -> classDef.sourceFile == "MainSettingItemType.kt" },
)

internal object SetupSettingsItemFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/util/List;",
    filters = listOf(
        fieldAccess(
            definingClass = { MainSettingItemTypeFingerprint.originalClassDef.type },
            name = "LABORATORY",
            opcode = Opcode.SGET_OBJECT,
        ),
    ),
    custom = { method, classDef ->
        classDef.sourceFile == "SettingActivity.kt" &&
            method.parameterTypeNames.firstOrNull() == "Landroid/content/Context;" &&
            "Landroid/content/Intent;" in method.parameterTypeNames
    },
)

internal object SettingsDividerFingerprint : Fingerprint(
    name = "<init>",
    parameters = List(5) { "I" },
    custom = { _, classDef -> classDef.sourceFile == "DividerItem.kt" },
)

private object AdaptiveLaboratoryItemFingerprint : Fingerprint(
    name = "<clinit>",
    strings = listOf("main.laboratory"),
    custom = { _, classDef -> classDef.sourceFile == "SettingItem.kt" },
) {
    val itemField get() = originalClassDef.staticFields.single { it.type == originalClassDef.type }
}

internal object AdaptiveSettingsListFingerprint : Fingerprint(
    parameters = emptyList(),
    returnType = "Ljava/util/List;",
    filters = listOf(
        fieldAccess(reference = { AdaptiveLaboratoryItemFingerprint.itemField }, opcode = Opcode.SGET_OBJECT),
        methodCall("Lkotlin/collections/CollectionsKt;->listOf([Ljava/lang/Object;)Ljava/util/List;"),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
    ),
    custom = { _, classDef -> classDef.sourceFile == "SettingMainPageModule.kt" },
) {
    val itemType get() = AdaptiveLaboratoryItemFingerprint.originalClassDef.interfaces.single()
}

internal object AdaptiveSettingsRouteFingerprint : Fingerprint(
    classFingerprint = AdaptiveSettingsListFingerprint,
    parameters = listOf("L", "Lkotlin/coroutines/Continuation;"),
    returnType = "Ljava/lang/Object;",
    filters = listOf(
        fieldAccess(reference = { AdaptiveLaboratoryItemFingerprint.itemField }, opcode = Opcode.SGET_OBJECT),
        fieldAccess(name = "Laboratory", opcode = Opcode.SGET_OBJECT),
    ),
)

internal object ExternalSettingRouteFingerprint : Fingerprint(
    name = "toString",
    parameters = emptyList(),
    returnType = "Ljava/lang/String;",
    strings = listOf("External(target=", ", tracker="),
    custom = { _, classDef -> classDef.sourceFile == "SettingRoute.kt" },
) {
    val constructor get() = originalClassDef.methods.single { it.name == "<init>" && it.parameters.size == 2 }
}

internal object AdaptiveSettingsOpenHandlerFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = { listOf(ExternalSettingRouteFingerprint.constructor.parameterTypes.first()) },
    returnType = "V",
    filters = listOf(fieldAccess(type = "Lcom/kakao/talk/activity/BaseActivity;", opcode = Opcode.IGET_OBJECT)),
    custom = { _, classDef -> classDef.sourceFile == "AdaptiveSettingOpenHandler.kt" },
)

internal object ThemePrefNightModeReadFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = emptyList(),
    returnType = "I",
    filters = listOf(
        methodCall(parameters = emptyList(), returnType = "L", opcodes = listOf(Opcode.INVOKE_VIRTUAL)),
        methodCall("Ljava/lang/Number;->intValue()I"),
    ),
    custom = { _, classDef -> classDef.sourceFile == "ThemePref.kt" },
)

internal object ThemePrefNightModeWriteFingerprint : Fingerprint(
    classFingerprint = ThemePrefNightModeReadFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("I"),
    returnType = "V",
    filters = listOf(
        methodCall(reference = { ThemePrefNightModeReadFingerprint.instructionMatches.first().methodReference }),
        methodCall("Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;"),
    ),
)
