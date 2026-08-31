package app.revanced.patches.samsungkeyboard.misc.nononeui

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.newInstance
import com.android.tools.smali.dexlib2.Opcode

private const val CLIPBOARD_HONEY_BEE_TYPE =
    "Lcom/samsung/android/honeyboard/icecone/board/ClipboardHoneyBee;"
private const val GIF_HONEY_BEE_TYPE =
    "Lcom/samsung/android/honeyboard/icecone/board/GifHoneyBee;"

private val storeRequestStrings = listOf("&deviceId=", "&abiType=", "&oneUiVersion=")

internal object StoreDownloadRequestFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    parameters = listOf(
        "Landroid/content/Context;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Z",
        "Z",
        "Z",
    ),
    strings = storeRequestStrings,
)

internal object StoreUpdateCheckRequestFingerprint : Fingerprint(
    returnType = "[I",
    parameters = listOf("Landroid/content/Context;", "I", "Ljava/lang/String;"),
    strings = storeRequestStrings,
)

internal object ShowSoftInputFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("I", "Landroid/content/Context;"),
    strings = listOf("showSoftInputInner flags="),
)

internal abstract class ToolbarFeatureFlagFingerprint(type: String) : Fingerprint(
    filters = listOf(
        fieldAccess(type = "Z", opcode = Opcode.SGET_BOOLEAN),
        newInstance(type, MatchAfterWithin(3)),
    ),
)

internal object ClipboardFeatureFlagFingerprint :
    ToolbarFeatureFlagFingerprint(CLIPBOARD_HONEY_BEE_TYPE)

internal object GifFeatureFlagFingerprint :
    ToolbarFeatureFlagFingerprint(GIF_HONEY_BEE_TYPE)

internal object GifVisibilityFingerprint : Fingerprint(
    definingClass = GIF_HONEY_BEE_TYPE,
    name = "updateBeeVisibility",
    returnType = "V",
    parameters = emptyList(),
    filters = listOf(
        methodCall(name = "setBeeVisibility", parameters = listOf("I"), returnType = "V"),
    ),
)