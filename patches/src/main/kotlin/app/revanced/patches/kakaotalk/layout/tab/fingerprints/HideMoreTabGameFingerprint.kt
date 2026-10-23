package app.revanced.patches.kakaotalk.layout.tab.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

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

internal fun moreTabPagerItemsFingerprint(gameType: String) = Fingerprint(
    returnType = "Ljava/util/List;",
    filters = listOf(
        fieldAccess(type = gameType, opcode = Opcode.SGET_OBJECT),
        opcode(Opcode.NEW_ARRAY),
        methodCall("Lkotlin/collections/CollectionsKt;->listOfNotNull([Ljava/lang/Object;)Ljava/util/List;"),
    ),
    custom = { _, classDef -> classDef.sourceFile == "MoreTabPagerViewModel.kt" },
)

internal fun moreTabPagerItemMapperFingerprint(gameType: String, pagerType: String) = Fingerprint(
    parameters = listOf("Ljava/lang/String;", "L"),
    returnType = pagerType,
    filters = listOf(
        fieldAccess(type = gameType, opcode = Opcode.SGET_OBJECT),
        opcode(Opcode.RETURN_OBJECT),
    ),
    custom = { _, classDef -> classDef.sourceFile == "MoreTabPagerViewModel.kt" },
)

internal fun moreTabPagerItemIdFingerprint(pagerType: String) = Fingerprint(
    definingClass = pagerType,
    parameters = emptyList(),
    returnType = "Ljava/lang/String;",
    filters = listOf(fieldAccess(type = "Ljava/lang/String;", opcode = Opcode.IGET_OBJECT)),
)

internal object MoreTabPagerSelectionFingerprint : Fingerprint(
    parameters = listOf("Ljava/lang/String;", "Landroid/net/Uri;"),
    returnType = "V",
    filters = listOf(methodCall("Ljava/util/Set;->contains(Ljava/lang/Object;)Z")),
    custom = { _, classDef -> classDef.sourceFile == "MoreTabPagerViewModel.kt" },
)