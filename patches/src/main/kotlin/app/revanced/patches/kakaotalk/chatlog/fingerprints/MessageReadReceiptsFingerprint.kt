package app.revanced.patches.kakaotalk.chatlog.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.util.getReference
import app.revanced.util.parameterTypeNames
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object ChatMemberSetConstructorFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    parameters = listOf(
        "J",
        "Ljava/util/List;",
        "Ljava/util/List;",
        "Ljava/util/Set;",
        "I",
    ),
    returnType = "V",
    custom = { method, classDef ->
        method.name == "<init>" && classDef.sourceFile == "ChatMemberSet.kt"
    },
)

internal object MemberWatermarksGetterFingerprint : Fingerprint(
    classFingerprint = ChatMemberSetConstructorFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = emptyList(),
    returnType = "Ljava/util/Map;",
)

internal fun activeMembersCountGetterFingerprint(activeMembersCountField: FieldReference) =
    object : Fingerprint(
        classFingerprint = ChatMemberSetConstructorFingerprint,
        accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
        parameters = emptyList(),
        returnType = "I",
        filters = listOf(
            fieldAccess(reference = activeMembersCountField),
        ),
    ) {}

internal object OpenLinkIdGetterFingerprint : Fingerprint(
    classFingerprint = ChatMemberSetConstructorFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = emptyList(),
    returnType = "J",
    filters = listOf(
        methodCall("Lkotlin/Lazy;->getValue()Ljava/lang/Object;"),
        methodCall("Ljava/lang/Number;->longValue()J"),
    ),
)

internal fun normalMemberProfileFingerprint(friendType: String) = object : Fingerprint(
    classFingerprint = ChatMemberSetConstructorFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = friendType,
    custom = { method, _ ->
        !AccessFlags.STATIC.isSet(method.accessFlags) &&
            method.parameterTypeNames.size == 2 &&
            method.parameterTypeNames[0] == "J" &&
            method.parameterTypeNames[1].startsWith("L")
    },
) {}

internal fun openMemberProfileFingerprint(friendType: String) = object : Fingerprint(
    classFingerprint = ChatMemberSetConstructorFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = friendType,
    custom = { method, _ ->
        !AccessFlags.STATIC.isSet(method.accessFlags) &&
            method.parameterTypeNames.size == 3 &&
            method.parameterTypeNames[0] == "J" &&
            method.parameterTypeNames[1] == "J" &&
            method.parameterTypeNames[2].startsWith("L")
    },
) {}

internal fun chatRoomMemberSetGetterFingerprint(
    chatRoomType: String,
    memberSetType: String,
) = object : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = emptyList(),
    returnType = memberSetType,
    custom = { method, classDef ->
        method.definingClass == chatRoomType && classDef.sourceFile == "ChatRoom.kt"
    },
) {}

internal object FriendPrimaryConstructorFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
    custom = { method, classDef ->
        method.name == "<init>" &&
            classDef.sourceFile == "Friend.kt" &&
            method.parameterTypeNames.size == 5 &&
            method.parameterTypeNames[0] == "J" &&
            method.parameterTypeNames[1] == "Ljava/lang/String;" &&
            method.parameterTypeNames[2] == "Ljava/lang/String;" &&
            method.parameterTypeNames[3].startsWith("L") &&
            method.parameterTypeNames[4] == "Z"
    },
)

internal object FriendProfileImageUrlFingerprint : Fingerprint(
    classFingerprint = FriendPrimaryConstructorFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    name = "getProfileImageUrl",
    parameters = emptyList(),
    returnType = "Ljava/lang/String;",
)

internal object FriendDisplayNameFingerprint : Fingerprint(
    classFingerprint = FriendPrimaryConstructorFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = emptyList(),
    returnType = "Ljava/lang/String;",
    custom = { method, classDef ->
        val instructions = method.implementation?.instructions?.toList() ?: emptyList<Instruction>()
        val referencedStringFields = instructions
            .mapNotNull { it.getReference<FieldReference>() }
            .filter { it.definingClass == classDef.type && it.type == "Ljava/lang/String;" }
            .map { it.name }
            .distinct()
        val blankChecks = instructions.count { instruction ->
            instruction.getReference<MethodReference>()?.let { reference ->
                reference.parameterTypeNames == listOf("Ljava/lang/CharSequence;") &&
                    reference.returnType == "Z"
            } == true
        }

        referencedStringFields.size >= 3 &&
            blankChecks >= 2 &&
            instructions.count { it.opcode == Opcode.RETURN_OBJECT } >= 3
    },
)

internal fun currentUserPredicateFingerprint(meType: String) = object : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = listOf("J"),
    returnType = "Z",
    filters = listOf(
        opcode(Opcode.INVOKE_VIRTUAL),
        opcode(Opcode.MOVE_RESULT_WIDE),
        opcode(Opcode.CMP_LONG),
    ),
    custom = { _, classDef ->
        meType in classDef.interfaces && classDef.sourceFile == "Me.kt"
    },
) {}
