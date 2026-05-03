package app.revanced.patches.dcinside.misc

import app.morphe.patcher.Fingerprint
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal const val OFFICIAL_NOTICE_EXTENSION_CLASS =
    "Lapp/revanced/extension/dcinside/patches/OfficialNoticePatch;"

internal object OfficialNoticeAuthorNameBridgeFingerprint : Fingerprint(
    definingClass = OFFICIAL_NOTICE_EXTENSION_CLASS,
    name = "getPostAuthorName",
)

internal object GalleryNoticeListHandlerFingerprint : Fingerprint(
    definingClass = "Lcom/dcinside/app/post/fragments",
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    returnType = "V",
    custom = custom@{ method, _ ->
        if (method.accessFlags and AccessFlags.STATIC.value != 0) {
            return@custom false
        }
        if (method.parameterTypes.size != 3 ||
            method.parameterTypes[1].toString() != "I"
        ) {
            return@custom false
        }

        val responseType = method.parameterTypes[0].toString()
        method.implementation?.instructions?.any { instruction ->
            val reference = instruction.getReference<MethodReference>()
                ?: return@any false

            reference.definingClass == responseType &&
                reference.parameterTypes.isEmpty() &&
                reference.returnType == "Ljava/util/List;"
        } == true
    },
)

internal object MiniNoticeAdapterItemsFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Ljava/util/List;", "J", "Ljava/lang/String;"),
    returnType = "V",
    strings = listOf("item", "gallId"),
)
