package app.revanced.patches.dcinside.layout.hide.officialnotice

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resourceLiteral
import app.revanced.patches.dcinside.layout.shortcut.PostListFragmentFingerprint
import app.morphe.patcher.util.getReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal const val OFFICIAL_NOTICE_EXTENSION_CLASS =
    "Lapp/revanced/extension/dcinside/patches/OfficialNoticePatch;"

internal object OfficialNoticeAuthorNameBridgeFingerprint : Fingerprint(
    definingClass = OFFICIAL_NOTICE_EXTENSION_CLASS,
    name = "getPostAuthorName",
)

internal object GalleryNoticeListHandlerFingerprint : Fingerprint(
    classFingerprint = PostListFragmentFingerprint,
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

private object MiniNoticeAdapterFingerprint : Fingerprint(
    name = "onCreateViewHolder",
    parameters = listOf("Landroid/view/ViewGroup;", "I"),
    returnType = "L",
    filters = listOf(resourceLiteral(ResourceType.LAYOUT, "view_mini_notice_single_item")),
)

internal object MiniNoticeAdapterItemsFingerprint : Fingerprint(
    classFingerprint = MiniNoticeAdapterFingerprint,
    parameters = listOf("J", "Ljava/lang/String;", "Ljava/util/List;"),
    returnType = "V",
)
