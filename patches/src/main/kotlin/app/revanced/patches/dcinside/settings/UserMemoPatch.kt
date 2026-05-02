package app.revanced.patches.dcinside.settings

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.util.getReference
import app.revanced.patches.dcinside.misc.addExtensionPatch
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

internal val userMemoPatch = bytecodePatch {
    compatibleWith(COMPATIBILITY_DC_INSIDE)
    dependsOn(
        addExtensionPatch,
        userMemoResourcesPatch,
    )

    execute {
        val classDefsByType = mutableMapOf<String, ClassDef>()
        classDefForEach { classDef ->
            classDefsByType[classDef.type] = classDef
        }

        val bridgeMethods = UserMemoBridgeMethods(
            openRealm = UserMemoOpenRealmFingerprint.method,
            newPairArray = UserMemoNewPairArrayFingerprint.method,
            newPair = UserMemoNewPairFingerprint.method,
            registerEntries = UserMemoRegisterEntriesFingerprint.method,
            beginTransaction = UserMemoBeginTransactionFingerprint.method,
            queryEntries = UserMemoQueryEntriesFingerprint.method,
            queryContainers = UserMemoQueryContainersFingerprint.method,
            queryCounts = UserMemoQueryCountsFingerprint.method,
            commitTransaction = UserMemoCommitTransactionFingerprint.method,
            transactionActive = UserMemoTransactionActiveFingerprint.method,
            cancelTransaction = UserMemoCancelTransactionFingerprint.method,
            reloadCache = UserMemoReloadCacheFingerprint.method,
            deleteRealmObject = UserMemoDeleteRealmObjectFingerprint.method,
        )

        UserMemoBindings
            .from(UserMemoRegisterFingerprint.method, classDefsByType)
            .inject(bridgeMethods)
    }
}

private data class UserMemoBridgeMethods(
    val openRealm: MutableMethod,
    val newPairArray: MutableMethod,
    val newPair: MutableMethod,
    val registerEntries: MutableMethod,
    val beginTransaction: MutableMethod,
    val queryEntries: MutableMethod,
    val queryContainers: MutableMethod,
    val queryCounts: MutableMethod,
    val commitTransaction: MutableMethod,
    val transactionActive: MutableMethod,
    val cancelTransaction: MutableMethod,
    val reloadCache: MutableMethod,
    val deleteRealmObject: MutableMethod,
)

private data class UserMemoBindings(
    val registerMethod: Method,
    val realmType: String,
    val pairArrayType: String,
    val pairType: String,
    val managerType: String,
    val defaultRealmMethod: String,
    val containerType: String,
    val countType: String,
    val entryType: String,
    val realmObjectType: String,
    val realmQueryMethod: String,
    val realmFindAllMethod: MethodReference,
    val beginTransactionMethod: String,
    val commitTransactionMethod: String,
    val cancelTransactionMethod: String,
    val inTransactionMethod: String,
    val managerInstanceFieldName: String,
    val cacheReloadMethodName: String,
    val realmObjectDeleteMethod: String,
) {
    fun inject(methods: UserMemoBridgeMethods) {
        methods.openRealm.addInstructions(
            0,
            """
                invoke-static {}, $realmType->$defaultRealmMethod()$realmType
                move-result-object p0
                return-object p0
            """.trimIndent(),
        )

        methods.newPairArray.addInstructions(
            0,
            """
                new-array p1, p0, $pairArrayType
                return-object p1
            """.trimIndent(),
        )

        methods.newPair.addInstructions(
            0,
            """
                new-instance p2, $pairType
                invoke-direct {p2, p0, p1}, $pairType-><init>(Ljava/lang/Object;Ljava/lang/Object;)V
                return-object p2
            """.trimIndent(),
        )

        methods.registerEntries.addInstructions(
            0,
            """
                check-cast p0, $realmType
                check-cast p1, $pairArrayType
                invoke-static {p0, p2, p3, p4, p1}, ${registerMethod.definingClass}->${registerMethod.name}(${realmType}Ljava/lang/String;Ljava/lang/String;Z${pairArrayType})Z
                move-result p0
                return p0
            """.trimIndent(),
        )

        methods.beginTransaction.addRealmVoidBridge(realmType, beginTransactionMethod)
        methods.queryEntries.addRealmQueryBridge(entryType)
        methods.queryContainers.addRealmQueryBridge(containerType)
        methods.queryCounts.addRealmQueryBridge(countType)
        methods.commitTransaction.addRealmVoidBridge(realmType, commitTransactionMethod)
        methods.cancelTransaction.addRealmVoidBridge(realmType, cancelTransactionMethod)

        methods.transactionActive.addInstructions(
            0,
            """
                check-cast p0, $realmType
                invoke-virtual {p0}, $realmType->$inTransactionMethod()Z
                move-result p0
                return p0
            """.trimIndent(),
        )

        methods.reloadCache.addInstructions(
            0,
            """
                check-cast p0, $realmType
                sget-object p1, $managerType->$managerInstanceFieldName:$managerType
                invoke-virtual {p1, p0}, $managerType->$cacheReloadMethodName($realmType)V
                return-void
            """.trimIndent(),
        )

        methods.deleteRealmObject.addInstructions(
            0,
            """
                check-cast p0, $realmObjectType
                invoke-virtual {p0}, $realmObjectType->$realmObjectDeleteMethod()V
                return-void
            """.trimIndent(),
        )
    }

    private fun MutableMethod.addRealmQueryBridge(modelType: String) {
        addInstructions(
            0,
            """
                check-cast p0, $realmType
                const-class p1, $modelType
                invoke-virtual {p0, p1}, $realmType->$realmQueryMethod(Ljava/lang/Class;)Lio/realm/RealmQuery;
                move-result-object p0
                invoke-virtual {p0}, Lio/realm/RealmQuery;->${realmFindAllMethod.name}()${realmFindAllMethod.returnType}
                move-result-object p0
                return-object p0
            """.trimIndent(),
        )
    }

    companion object {
        fun from(
            registerMethod: Method,
            classDefsByType: Map<String, ClassDef>,
        ): UserMemoBindings {
            val registerInstructions = registerMethod.implementation?.instructions
                ?: throw PatchException("Could not inspect user memo register method")
            val registerMethodReferences = registerInstructions
                .mapNotNull { instruction -> instruction.getReference<MethodReference>() }

            val realmType = registerMethod.parameterTypes[0].toString()
            val pairArrayType = registerMethod.parameterTypes[4].toString()
            val managerType = registerMethod.definingClass
            val managerClass = classDefsByType.requireClass(managerType)
            val containerType = registerMethodReferences.inferMemoContainerType(managerType, realmType)
            val realmObjectType = classDefsByType.requireClass(containerType).superclass
                ?: throw PatchException("Could not infer user memo Realm object base type")

            val countType = registerInstructions
                .referencedTypes(Opcode.CONST_CLASS)
                .firstOrNull { type ->
                    type != containerType &&
                        classDefsByType[type]?.superclass == realmObjectType
                } ?: throw PatchException("Could not infer user memo count type")

            val entryType = registerInstructions
                .referencedTypes(Opcode.NEW_INSTANCE)
                .firstOrNull { type ->
                    type != containerType &&
                        type != countType &&
                        classDefsByType[type]?.superclass == realmObjectType
                } ?: throw PatchException("Could not infer user memo entry type")

            val transactionMethods = registerMethodReferences.inferTransactionMethods(realmType)
            val deleteMethod = classDefsByType.inferRealmObjectDeleteMethod(realmObjectType)

            return UserMemoBindings(
                registerMethod = registerMethod,
                realmType = realmType,
                pairArrayType = pairArrayType,
                pairType = pairArrayType.removePrefix("["),
                managerType = managerType,
                defaultRealmMethod = classDefsByType.inferDefaultRealmMethod(realmType),
                containerType = containerType,
                countType = countType,
                entryType = entryType,
                realmObjectType = realmObjectType,
                realmQueryMethod = registerMethodReferences.inferRealmQueryMethod(realmType),
                realmFindAllMethod = registerMethodReferences.inferRealmFindAllMethod(),
                beginTransactionMethod = transactionMethods.begin,
                commitTransactionMethod = transactionMethods.commit,
                cancelTransactionMethod = transactionMethods.cancel,
                inTransactionMethod = transactionMethods.isActive,
                managerInstanceFieldName = managerClass.inferSingletonField(managerType),
                cacheReloadMethodName = managerClass.inferCacheReloadMethod(realmType),
                realmObjectDeleteMethod = deleteMethod,
            )
        }
    }
}

private data class RealmTransactionMethods(
    val begin: String,
    val commit: String,
    val cancel: String,
    val isActive: String,
)

private fun MutableMethod.addRealmVoidBridge(
    realmType: String,
    methodName: String,
) {
    addInstructions(
        0,
        """
            check-cast p0, $realmType
            invoke-virtual {p0}, $realmType->$methodName()V
            return-void
        """.trimIndent(),
    )
}

private fun Iterable<Instruction>.referencedTypes(opcode: Opcode): List<String> =
    mapNotNull { instruction ->
        if (instruction.opcode != opcode) return@mapNotNull null

        instruction.getReference<TypeReference>()?.type
    }

private fun Map<String, ClassDef>.requireClass(type: String): ClassDef =
    this[type] ?: throw PatchException("Could not find class definition for $type")

private fun Map<String, ClassDef>.inferDefaultRealmMethod(realmType: String): String =
    requireClass(realmType).methods.singleOrNull { method ->
        method.parameterTypes.isEmpty() &&
            method.returnType == realmType &&
            method.isStatic
    }?.name ?: throw PatchException("Could not find default Realm opener for $realmType")

private fun Map<String, ClassDef>.inferRealmObjectDeleteMethod(realmObjectType: String): String {
    val realmObjectClass = requireClass(realmObjectType)
    val deleteStaticMethod = realmObjectClass.methods.singleOrNull { method ->
        method.parameterTypes.singleOrNull()?.toString() == "Lio/realm/X0;" &&
            method.returnType == "V" &&
            method.isStatic &&
            method.hasString("Object malformed: missing Realm. Make sure to instantiate RealmObjects with Realm.createObject()")
    } ?: throw PatchException("Could not infer Realm object static delete method")

    return realmObjectClass.methods.singleOrNull { method ->
        method.parameterTypes.isEmpty() &&
            method.returnType == "V" &&
            !method.isStatic &&
            method.hasMethodReference(deleteStaticMethod.definingClass, deleteStaticMethod.name)
    }?.name ?: throw PatchException("Could not infer Realm object delete method")
}

private fun List<MethodReference>.inferMemoContainerType(
    managerType: String,
    realmType: String,
): String = firstOrNull { reference ->
    reference.definingClass == managerType &&
        reference.parameterTypes.size == 2 &&
        reference.parameterTypes[0].toString() == realmType &&
        reference.parameterTypes[1].toString() == "Ljava/lang/String;" &&
        reference.returnType.startsWith("L")
}?.returnType ?: throw PatchException("Could not infer user memo container type")

private fun List<MethodReference>.inferRealmQueryMethod(realmType: String): String =
    firstOrNull { reference ->
        reference.definingClass == realmType &&
            reference.parameterTypes.singleOrNull()?.toString() == "Ljava/lang/Class;" &&
            reference.returnType == "Lio/realm/RealmQuery;"
    }?.name ?: throw PatchException("Could not infer Realm query method")

private fun List<MethodReference>.inferRealmFindAllMethod(): MethodReference =
    firstOrNull { reference ->
        reference.definingClass == "Lio/realm/RealmQuery;" &&
            reference.parameterTypes.isEmpty() &&
            reference.returnType.startsWith("Lio/realm/") &&
            reference.returnType != "Lio/realm/RealmQuery;"
    } ?: throw PatchException("Could not infer Realm findAll method")

private fun List<MethodReference>.inferTransactionMethods(realmType: String): RealmTransactionMethods {
    val begin = firstOrNull { reference ->
        reference.definingClass == realmType &&
            reference.name == "beginTransaction" &&
            reference.parameterTypes.isEmpty() &&
            reference.returnType == "V"
    }?.name ?: throw PatchException("Could not infer Realm beginTransaction method")

    val noArgumentVoidMethods = filter { reference ->
        reference.definingClass == realmType &&
            reference.parameterTypes.isEmpty() &&
            reference.returnType == "V" &&
            reference.name != begin
    }.map { reference -> reference.name }.distinct()

    val stateMethodCallCounts = linkedMapOf<String, Int>()
    filter { reference ->
        reference.definingClass == realmType &&
            reference.parameterTypes.isEmpty() &&
            reference.returnType == "Z" &&
            reference.name != "isClosed"
    }
        .forEach { reference ->
            stateMethodCallCounts[reference.name] = stateMethodCallCounts.getOrDefault(reference.name, 0) + 1
        }
    val isActive = stateMethodCallCounts.maxByOrNull { (_, count) -> count }
        ?.key ?: throw PatchException("Could not infer Realm transaction state method")

    return RealmTransactionMethods(
        begin = begin,
        commit = noArgumentVoidMethods.getOrNull(0)
            ?: throw PatchException("Could not infer Realm commit transaction method"),
        cancel = noArgumentVoidMethods.getOrNull(1)
            ?: throw PatchException("Could not infer Realm cancel transaction method"),
        isActive = isActive,
    )
}

private fun ClassDef.inferSingletonField(type: String): String =
    fields.firstOrNull { field ->
        field.type == type &&
            field.accessFlags and AccessFlags.STATIC.value != 0
    }?.name ?: throw PatchException("Could not infer user memo manager instance field")

private fun ClassDef.inferCacheReloadMethod(realmType: String): String =
    methods.singleOrNull { method ->
        method.parameterTypes.size == 1 &&
            method.parameterTypes[0].toString() == realmType &&
            method.returnType == "V" &&
            !method.isStatic &&
            method.hasString("findAll(...)")
    }?.name ?: throw PatchException("Could not infer user memo cache reload method")

private fun Method.hasString(string: String): Boolean =
    implementation?.instructions?.any { instruction ->
        instruction.getReference<StringReference>()?.string == string
    } == true

private fun Method.hasMethodReference(
    definingClass: String,
    name: String,
): Boolean = implementation?.instructions?.any { instruction ->
    val reference = instruction.getReference<MethodReference>() ?: return@any false
    reference.definingClass == definingClass && reference.name == name
} == true

private val Method.isStatic
    get() = accessFlags and AccessFlags.STATIC.value != 0
