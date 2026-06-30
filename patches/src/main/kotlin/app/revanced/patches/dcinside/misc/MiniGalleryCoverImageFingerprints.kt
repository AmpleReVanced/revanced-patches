package app.revanced.patches.dcinside.misc

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.util.getReference
import app.revanced.util.parameterTypeNames
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val GALLERY_INFO_TYPE = "Lcom/dcinside/app/response/GalleryInfo;"
private const val VIEW_TYPE = "Landroid/view/View;"
private const val VIEW_STUB_TYPE = "Landroid/view/ViewStub;"

internal object MiniGalleryHeaderSetupFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    parameters = emptyList(),
    returnType = "V",
    filters = listOf(
        methodCall(
            definingClass = GALLERY_INFO_TYPE,
            parameters = emptyList(),
            returnType = "L",
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        methodCall(
            parameters = emptyList(),
            returnType = "Z",
            location = MatchAfterImmediately(),
        ),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
        methodCall("$VIEW_TYPE->setVisibility(I)V"),
        methodCall("$VIEW_STUB_TYPE->setVisibility(I)V"),
    ),
    custom = { method, classDef ->
        method.findMiniGalleryTypeCheck() != null &&
            classDef.hasMiniGalleryHeaderHideMethod(method.definingClass)
    },
)

internal object MiniGalleryHeaderRestoreFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = emptyList(),
    returnType = "V",
    filters = listOf(
        methodCall(
            definingClass = GALLERY_INFO_TYPE,
            parameters = emptyList(),
            returnType = "L",
        ),
        literal(2),
        literal(0),
        methodCall("$VIEW_TYPE->setVisibility(I)V"),
        methodCall("$VIEW_STUB_TYPE->setVisibility(I)V"),
    ),
    custom = { method, _ ->
        method.isMiniGalleryHeaderRestoreMethod()
    },
)

internal data class MiniGalleryTypeCheck(
    val galleryInfoMethod: MethodReference,
    val galleryTypeCheckMethod: MethodReference,
    val moveResultIndex: Int,
)

internal fun Method.findMiniGalleryTypeCheck(): MiniGalleryTypeCheck? {
    val instructions = implementation?.instructions?.toList() ?: return null

    for (index in 0 until instructions.size - 3) {
        val galleryInfoReference = instructions[index].getReference<MethodReference>() ?: continue
        if (galleryInfoReference.definingClass != GALLERY_INFO_TYPE ||
            galleryInfoReference.parameterTypeNames.isNotEmpty() ||
            !galleryInfoReference.returnType.startsWith("L")
        ) {
            continue
        }

        if (instructions[index + 1].opcode != Opcode.MOVE_RESULT_OBJECT) {
            continue
        }

        val galleryTypeCheckReference = instructions[index + 2].getReference<MethodReference>() ?: continue
        if (galleryTypeCheckReference.definingClass != galleryInfoReference.returnType ||
            galleryTypeCheckReference.parameterTypeNames.isNotEmpty() ||
            galleryTypeCheckReference.returnType != "Z"
        ) {
            continue
        }

        if (instructions[index + 3].opcode == Opcode.MOVE_RESULT) {
            return MiniGalleryTypeCheck(
                galleryInfoReference,
                galleryTypeCheckReference,
                index + 3,
            )
        }
    }

    return null
}

internal fun ClassDef.findMiniGalleryHeaderHideMethod(ownerClass: String) =
    methods.singleOrNull { it.isMiniGalleryHeaderHideMethod(ownerClass) }

internal fun Method.findGalleryInfoFieldReference(): FieldReference? {
    val instructions = implementation?.instructions ?: return null

    return instructions
        .mapNotNull { it.getReference<FieldReference>() }
        .firstOrNull {
            it.definingClass == definingClass &&
                it.type == GALLERY_INFO_TYPE
        }
}

private fun ClassDef.hasMiniGalleryHeaderHideMethod(ownerClass: String) =
    methods.count { it.isMiniGalleryHeaderHideMethod(ownerClass) } == 1

private fun Method.isMiniGalleryHeaderHideMethod(ownerClass: String): Boolean {
    if (parameterTypes.isNotEmpty() || returnType != "V") {
        return false
    }
    if (!AccessFlags.PRIVATE.isSet(accessFlags) || !AccessFlags.FINAL.isSet(accessFlags)) {
        return false
    }

    val instructions = implementation?.instructions?.toList() ?: return false
    val references = instructions.mapNotNull { it.getReference<MethodReference>() }

    if (references.any { it.definingClass == GALLERY_INFO_TYPE }) {
        return false
    }

    val hidesRegularView = references.any {
        it.isSetVisibilityReference(VIEW_TYPE)
    }
    val hidesViewStub = references.any {
        it.isSetVisibilityReference(VIEW_STUB_TYPE)
    }
    val callsBooleanStateHelpers = references.count {
        it.definingClass == ownerClass &&
            it.parameterTypeNames == listOf("Z") &&
            it.returnType == "V"
    } >= 2

    return hidesRegularView &&
        hidesViewStub &&
        callsBooleanStateHelpers &&
        instructions.hasLiteral(8) &&
        instructions.hasLiteral(1) &&
        instructions.hasLiteral(0)
}

private fun Method.isMiniGalleryHeaderRestoreMethod(): Boolean {
    val instructions = implementation?.instructions?.toList() ?: return false
    val references = instructions.mapNotNull { it.getReference<MethodReference>() }
    val galleryType = references.firstOrNull {
        it.definingClass == GALLERY_INFO_TYPE &&
            it.parameterTypeNames.isEmpty() &&
            it.returnType.startsWith("L")
    }?.returnType ?: return false
    val fieldReferences = instructions.mapNotNull { it.getReference<FieldReference>() }

    val checksMiniAndPersonGalleryTypes = fieldReferences.count {
        it.definingClass == galleryType && it.type == galleryType
    } >= 2
    val callsHeaderStateHelpers = references.count {
        it.definingClass == definingClass &&
            it.parameterTypeNames == listOf("Z") &&
            it.returnType == "V"
    } >= 2

    return findGalleryInfoFieldReference() != null &&
        checksMiniAndPersonGalleryTypes &&
        callsHeaderStateHelpers
}

private fun MethodReference.isSetVisibilityReference(definingClass: String) =
    this.definingClass == definingClass &&
        name == "setVisibility" &&
        parameterTypeNames == listOf("I") &&
        returnType == "V"

private fun List<Instruction>.hasLiteral(value: Int) =
    any { (it as? NarrowLiteralInstruction)?.narrowLiteral == value }
