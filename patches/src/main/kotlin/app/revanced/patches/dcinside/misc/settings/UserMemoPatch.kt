package app.revanced.patches.dcinside.misc.settings

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.classDefByOrNull
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.misc.settings.preference.NonInteractivePreference
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.dcinside.misc.extension.addExtensionPatch
import app.revanced.util.smaliReference
import app.revanced.util.parameterRegister
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private const val EXTENSION_CLASS =
    "Lapp/revanced/extension/dcinside/settings/UserMemoPatch;"
private typealias ClassLookup = (String) -> ClassDef?

internal val userMemoPatch = patch {
    compatibleWith(COMPATIBILITY_DC_INSIDE)
    dependsOn(
        addExtensionPatch,
        userMemoResourcesPatch,
    )

    execute {
        PreferenceScreen.FEATURES.addPreferences(
            NonInteractivePreference(
                key = "morphe_pref_apply_user_memo_preset",
                titleKey = "morphe_settings_user_memo_preset",
                summaryKey = "morphe_settings_user_memo_preset_summary",
                selectable = true,
            ),
        )
        PreferenceScreen.MANAGE.addPreferences(
            NonInteractivePreference(
                key = "morphe_pref_clear_user_memos",
                titleKey = "morphe_settings_user_memo_clear",
                summaryKey = "morphe_settings_user_memo_clear_summary",
                selectable = true,
            ),
        )
        setExtensionIsPatchIncluded(EXTENSION_CLASS)

        val bridgeMethods = UserMemoBridgeMethods(
            openRealm = UserMemoOpenRealmFingerprint.method,
            newPairArray = UserMemoNewPairArrayFingerprint.method,
            newPair = UserMemoNewPairFingerprint.method,
            registerEntries = UserMemoRegisterEntriesFingerprint.method,
            memoPairs = UserMemoPairsFingerprint.method,
            memoText = UserMemoTextFingerprint.method,
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
            .from(UserMemoRegisterFingerprint.method) { classDefByOrNull(it) }
            .inject(bridgeMethods)
    }
}

private data class UserMemoBridgeMethods(
    val openRealm: MutableMethod,
    val newPairArray: MutableMethod,
    val newPair: MutableMethod,
    val registerEntries: MutableMethod,
    val memoPairs: MutableMethod,
    val memoText: MutableMethod,
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
    val memoPairsMethod: MethodReference,
    val pairTextField: FieldReference,
    val defaultRealmMethod: String,
    val containerType: String,
    val countType: String,
    val entryType: String,
    val realmObjectType: String,
    val realmModelType: String,
    val realmQueryMethod: String,
    val realmFindAllMethod: MethodReference,
    val beginTransactionMethod: String,
    val commitTransactionMethod: String,
    val cancelTransactionMethod: String,
    val inTransactionMethod: String,
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
                invoke-static {p0, p2, p3, p4, p1}, ${registerMethod.definingClass}->${registerMethod.name}(${realmType}Ljava/lang/String;Ljava/lang/String;Z${pairArrayType})V
                return-void
            """.trimIndent(),
        )

        methods.memoPairs.addInstructions(
            0,
            """
                check-cast p0, $realmType
                invoke-static {p0, p1}, $memoPairsMethod
                move-result-object p0
                return-object p0
            """,
        )
        methods.memoText.addInstructions(
            0,
            """
                check-cast p0, $pairType
                iget-object p0, p0, ${pairTextField.smaliReference}
                check-cast p0, Ljava/lang/String;
                return-object p0
            """,
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
                invoke-static {p0}, $managerType->$cacheReloadMethodName($realmType)V
                return-void
            """.trimIndent(),
        )

        methods.deleteRealmObject.addInstructions(
            0,
            """
                check-cast p0, $realmObjectType
                invoke-static {p0}, $realmObjectType->$realmObjectDeleteMethod($realmModelType)V
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
            classLookup: ClassLookup,
        ): UserMemoBindings {
            val registerInstructions = registerMethod.implementation?.instructions
                ?: throw PatchException("Could not inspect user memo register method")
            val registerMethodReferences = registerInstructions
                .mapNotNull { instruction -> instruction.getReference<MethodReference>() }

            val realmType = registerMethod.parameterTypes[0].toString()
            val pairArrayType = registerMethod.parameterTypes[4].toString()
            val managerType = registerMethod.definingClass
            val managerClass = classLookup.requireClass(managerType)
            val containerType = registerMethodReferences.inferMemoContainerType(managerType, realmType)
            val realmObjectType = classLookup.requireClass(containerType).superclass
                ?: throw PatchException("Could not infer user memo Realm object base type")

            val countType = registerInstructions
                .referencedTypes(Opcode.CONST_CLASS)
                .firstOrNull { type ->
                    type != containerType &&
                        classLookup(type)?.superclass == realmObjectType
                } ?: throw PatchException("Could not infer user memo count type")

            val entryType = registerInstructions
                .referencedTypes(Opcode.NEW_INSTANCE)
                .firstOrNull { type ->
                    type != containerType &&
                        type != countType &&
                        classLookup(type)?.superclass == realmObjectType
                } ?: throw PatchException("Could not infer user memo entry type")

            val transactionOwners = generateSequence(realmType) { classLookup(it)?.superclass }.toSet()
            val transactionMethods = registerMethodReferences.inferTransactionMethods(transactionOwners)
            val deleteMethod = classLookup.inferRealmObjectDeleteMethod(realmObjectType)

            return UserMemoBindings(
                registerMethod = registerMethod,
                realmType = realmType,
                pairArrayType = pairArrayType,
                pairType = pairArrayType.removePrefix("["),
                managerType = managerType,
                memoPairsMethod = registerMethodReferences.distinctBy { it.smaliReference }.single {
                    it.definingClass == managerType && it.parameterTypes == listOf(realmType, "Ljava/lang/String;") &&
                        it.returnType == "Ljava/util/HashMap;"
                },
                pairTextField = classLookup.requireClass(pairArrayType.removePrefix("[")).methods.single {
                    it.name == "<init>" && it.parameterTypes == listOf("Ljava/lang/Object;", "Ljava/lang/Object;")
                }.run {
                    val first = parameterRegister(0)
                    implementation!!.instructions.firstNotNullOf {
                        it.getReference<FieldReference>()?.takeIf { _ ->
                            it.opcode == Opcode.IPUT_OBJECT && (it as TwoRegisterInstruction).registerA == first
                        }
                    }
                },
                defaultRealmMethod = classLookup.inferDefaultRealmMethod(realmType),
                containerType = containerType,
                countType = countType,
                entryType = entryType,
                realmObjectType = realmObjectType,
                realmModelType = classLookup.requireClass(realmObjectType).interfaces.single(),
                realmQueryMethod = registerMethodReferences.inferRealmQueryMethod(realmType),
                realmFindAllMethod = registerMethodReferences.inferRealmFindAllMethod(classLookup),
                beginTransactionMethod = transactionMethods.begin,
                commitTransactionMethod = transactionMethods.commit,
                cancelTransactionMethod = transactionMethods.cancel,
                inTransactionMethod = transactionMethods.isActive,
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

private fun ClassLookup.requireClass(type: String): ClassDef =
    this(type) ?: throw PatchException("Could not find class definition for $type")

private fun ClassLookup.inferDefaultRealmMethod(realmType: String): String =
    requireClass(realmType).methods.singleOrNull { method ->
        method.parameterTypes.isEmpty() &&
            method.returnType == realmType &&
            method.isStatic
    }?.name ?: throw PatchException("Could not find default Realm opener for $realmType")

private fun ClassLookup.inferRealmObjectDeleteMethod(realmObjectType: String): String =
    requireClass(realmObjectType).methods.single { method ->
        method.parameterTypes.size == 1 && method.returnType == "V" && method.isStatic &&
            method.hasString("Object malformed: missing Realm. Make sure to instantiate RealmObjects with Realm.createObject()")
    }.name

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

private fun List<MethodReference>.inferRealmFindAllMethod(classLookup: ClassLookup): MethodReference =
    firstOrNull { reference ->
        reference.definingClass == "Lio/realm/RealmQuery;" &&
            reference.parameterTypes.isEmpty() &&
            classLookup(reference.returnType)?.superclass == "Ljava/util/AbstractList;"
    } ?: throw PatchException("Could not infer Realm findAll method")

private fun List<MethodReference>.inferTransactionMethods(realmTypes: Set<String>): RealmTransactionMethods {
    val begin = firstOrNull { reference ->
        reference.definingClass in realmTypes &&
            reference.name == "beginTransaction" &&
            reference.parameterTypes.isEmpty() &&
            reference.returnType == "V"
    }?.name ?: throw PatchException("Could not infer Realm beginTransaction method")

    val noArgumentVoidMethods = filter { reference ->
        reference.definingClass in realmTypes &&
            reference.parameterTypes.isEmpty() &&
            reference.returnType == "V" &&
            reference.name != begin
    }.map { reference -> reference.name }.distinct()

    val stateMethodCallCounts = linkedMapOf<String, Int>()
    filter { reference ->
        reference.definingClass in realmTypes &&
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

private fun ClassDef.inferCacheReloadMethod(realmType: String): String =
    methods.singleOrNull { method ->
        method.parameterTypes.size == 1 &&
            method.parameterTypes[0].toString() == realmType &&
            method.returnType == "V" &&
            method.isStatic &&
            method.hasMethodReference("Ljava/util/HashMap;", "putAll")
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
