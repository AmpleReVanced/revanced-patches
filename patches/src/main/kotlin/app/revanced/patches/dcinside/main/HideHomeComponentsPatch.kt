package app.revanced.patches.dcinside.main

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.util.getReference
import app.morphe.util.setExtensionIsPatchIncluded
import app.revanced.patches.dcinside.settings.PreferenceScreen
import app.revanced.patches.dcinside.settings.addSettingsPatch
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OffsetInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.SwitchPayload
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.value.IntEncodedValue

private const val EXTENSION_CLASS = "Lapp/revanced/extension/dcinside/patches/HomeComponentsPatch;"
private const val CONTEXT_TYPE = "Landroid/content/Context;"
private const val ATTRIBUTE_SET_TYPE = "Landroid/util/AttributeSet;"
private const val VIEW_GROUP_TYPE = "Landroid/view/ViewGroup;"
private const val RECYCLER_VIEW_HOLDER_TYPE = "Landroidx/recyclerview/widget/RecyclerView\$ViewHolder;"
private const val KOTLIN_DEFAULT_MARKER_TYPE = "Lkotlin/jvm/internal/w;"

@Suppress("unused")
val hideHomeComponentsPatch = bytecodePatch(
    name = "Hide home components",
    description = "Adds settings to hide selected DCInside home screen components.",
) {
    compatibleWith(COMPATIBILITY_DC_INSIDE)
    dependsOn(addSettingsPatch)

    execute {
        PreferenceScreen.HOME_SCREEN.addPreferences(
            SwitchPreference(
                key = "morphe_pref_hide_home_search_menu",
                titleKey = "morphe_settings_hide_home_search_menu",
                summary = true,
            ),
            SwitchPreference(
                key = "morphe_pref_hide_home_recent_galleries",
                titleKey = "morphe_settings_hide_home_recent_galleries",
                summary = true,
            ),
            SwitchPreference(
                key = "morphe_pref_hide_home_recommended_galleries",
                titleKey = "morphe_settings_hide_home_recommended_galleries",
                summary = true,
            ),
            SwitchPreference(
                key = "morphe_pref_hide_home_gallery_ranking",
                titleKey = "morphe_settings_hide_home_gallery_ranking",
                summary = true,
            ),
            SwitchPreference(
                key = "morphe_pref_hide_home_live_best",
                titleKey = "morphe_settings_hide_home_live_best",
                summary = true,
            ),
            SwitchPreference(
                key = "morphe_pref_hide_home_recommended_posts",
                titleKey = "morphe_settings_hide_home_recommended_posts",
                summary = true,
            ),
        )
        setExtensionIsPatchIncluded(EXTENSION_CLASS)

        val updateItemsMethod = MainAdapterItemsUpdateFingerprint.method
        val adapterClass = MainAdapterItemsUpdateFingerprint.classDef
        val itemTypeGetter = adapterClass.methods.inferItemTypeGetter(adapterClass.type)
        val onCreateViewHolderMethod = adapterClass.methods.inferOnCreateViewHolderMethod()
        val componentTypes = onCreateViewHolderMethod.inferHomeComponentTypes(
            context = this,
            resources = homeComponentResources(),
        )

        updateItemsMethod.addInstructions(
            0,
            """
                const-string v0, "${itemTypeGetter.name.toSmaliStringLiteral()}"
                const-string v1, "${componentTypes.toTypeSpec()}"
                invoke-static {p1, v0, v1}, $EXTENSION_CLASS->filterMainItems(Ljava/util/List;Ljava/lang/String;Ljava/lang/String;)Ljava/util/List;
                move-result-object p1
            """.trimIndent(),
        )
    }
}

private data class HomeComponentResources(
    val searchLayouts: Set<Int>,
    val recentLayouts: Set<Int>,
    val recommendedGalleriesLayout: Int,
    val liveFilterLayout: Int,
    val liveItemLayout: Int,
    val liveMoreLayout: Int,
    val liveRankingLayout: Int,
    val recommendedTitleLayout: Int,
    val recommendedItemLayout: Int,
)

private data class HomeComponentTypes(
    val searchMenu: Int,
    val recentGalleries: Int,
    val recommendedGalleries: Int,
    val galleryRanking: Int,
    val liveFilter: Int,
    val liveItem: Int,
    val liveMore: Int,
    val liveRanking: Int,
    val recommendedTitle: Int,
    val recommendedItem: Int,
) {
    fun toTypeSpec(): String = listOf(
        listOf(searchMenu),
        listOf(recentGalleries),
        listOf(recommendedGalleries),
        listOf(galleryRanking),
        listOf(liveFilter, liveItem, liveMore, liveRanking),
        listOf(recommendedTitle, recommendedItem),
    ).joinToString(";") { group -> group.sorted().joinToString(",") }
}

private data class ItemTypeBranch(
    val type: Int,
    val startIndex: Int,
)

private data class BranchBlock(
    val type: Int,
    val instructions: List<Instruction>,
) {
    val methodReferences: List<MethodReference> =
        instructions.mapNotNull { it.getReference<MethodReference>() }

    val literalValues: Set<Int> =
        instructions.mapNotNull { it.narrowLiteralRegister()?.second }.toSet()

    fun hasDirectLayout(layoutId: Int) = layoutId in literalValues

    fun hasAnyDirectLayout(layoutIds: Set<Int>) = layoutIds.any(::hasDirectLayout)

    fun hasConstructorWithParameters(vararg parameterTypes: String) =
        methodReferences.any { reference ->
            reference.name == "<init>" &&
                reference.parameterTypes.map { it.toString() } == parameterTypes.toList()
        }
}

private fun BytecodePatchContext.homeComponentResources() = HomeComponentResources(
    searchLayouts = setOf(
        layoutResourceId("view_main_search_gallery_basic"),
        layoutResourceId("view_main_search_gallery_split"),
    ),
    recentLayouts = setOf(
        layoutResourceId("view_recent_basic"),
        layoutResourceId("view_recent_split"),
    ),
    recommendedGalleriesLayout = layoutResourceId("view_recommend_galleries"),
    liveFilterLayout = layoutResourceId("view_main_best_filter"),
    liveItemLayout = layoutResourceId("view_live_best_item"),
    liveMoreLayout = layoutResourceId("view_main_live_best_more"),
    liveRankingLayout = layoutResourceId("view_live_best_ranking_item"),
    recommendedTitleLayout = layoutResourceId("view_main_best_pick_title"),
    recommendedItemLayout = layoutResourceId("view_best_pick_item"),
)

private fun BytecodePatchContext.layoutResourceId(name: String): Int {
    val field = classDefByOrNull("Lcom/dcinside/app/R\$layout;")
        ?.fields
        ?.firstOrNull { it.name == name }
        ?: throw PatchException("Could not find layout resource $name")

    return (field.initialValue as? IntEncodedValue)?.value
        ?: throw PatchException("Could not read layout resource id for $name")
}

private fun Iterable<MutableMethod>.inferItemTypeGetter(adapterClass: String): MethodReference {
    val candidates = mapNotNull { method ->
        if (method.parameterTypes.map { it.toString() } != listOf("I") || method.returnType != "I") {
            return@mapNotNull null
        }

        val references = method.instructions.mapNotNull { it.getReference<MethodReference>() }
        val readsAdapterItems = references.any { reference ->
            reference.definingClass == "Ljava/util/ArrayList;" &&
                reference.name == "get" &&
                reference.parameterTypes.singleOrNull()?.toString() == "I" &&
                reference.returnType == "Ljava/lang/Object;"
        }
        if (!readsAdapterItems) return@mapNotNull null

        references.singleOrNull { reference ->
            reference.definingClass != adapterClass &&
                reference.parameterTypes.isEmpty() &&
                reference.returnType == "I"
        }
    }.distinctBy { it.definingClass to it.name }

    return candidates.singleOrNull()
        ?: throw PatchException("Could not infer main adapter item type getter")
}

private fun Iterable<MutableMethod>.inferOnCreateViewHolderMethod(): MutableMethod =
    singleOrNull { method ->
        method.parameterTypes.map { it.toString() } == listOf(VIEW_GROUP_TYPE, "I") &&
            method.returnType == RECYCLER_VIEW_HOLDER_TYPE &&
            method.instructions.any { instruction ->
                instruction.getReference<MethodReference>()?.let { reference ->
                    reference.definingClass == "Landroid/view/LayoutInflater;" &&
                        reference.name == "from"
                } == true
            }
    } ?: throw PatchException("Could not infer MainAdapter.onCreateViewHolder")

private fun MutableMethod.inferHomeComponentTypes(
    context: BytecodePatchContext,
    resources: HomeComponentResources,
): HomeComponentTypes {
    val branches = buildBranchBlocks(inferViewTypeBranches())

    fun findType(name: String, predicate: (BranchBlock) -> Boolean) =
        branches.singleOrNull(predicate)?.type
            ?: throw PatchException("Could not infer $name item type")

    return HomeComponentTypes(
        searchMenu = findType("home search/menu") {
            it.hasAnyDirectLayout(resources.searchLayouts)
        },
        recentGalleries = findType("recent galleries") {
            it.hasAnyLayoutViaConstructor(context, resources.recentLayouts)
        },
        recommendedGalleries = findType("recommended galleries") {
            it.hasDirectLayout(resources.recommendedGalleriesLayout)
        },
        galleryRanking = findType("gallery ranking") {
            it.hasConstructorWithParameters(
                CONTEXT_TYPE,
                ATTRIBUTE_SET_TYPE,
                "I",
                "I",
                KOTLIN_DEFAULT_MARKER_TYPE,
            )
        },
        liveFilter = findType("live best filter") {
            it.hasLayoutViaConstructor(context, resources.liveFilterLayout)
        },
        liveItem = findType("live best item") {
            it.hasLayoutViaConstructor(context, resources.liveItemLayout)
        },
        liveMore = findType("live best more") {
            it.hasDirectLayout(resources.liveMoreLayout)
        },
        liveRanking = findType("live best ranking item") {
            it.hasLayoutViaConstructor(context, resources.liveRankingLayout)
        },
        recommendedTitle = findType("recommended posts title") {
            it.hasDirectLayout(resources.recommendedTitleLayout)
        },
        recommendedItem = findType("recommended posts item") {
            it.hasLayoutViaConstructor(context, resources.recommendedItemLayout)
        },
    )
}

private fun BranchBlock.hasLayoutViaConstructor(context: BytecodePatchContext, layoutId: Int) =
    hasAnyLayoutViaConstructor(context, setOf(layoutId))

private fun BranchBlock.hasAnyLayoutViaConstructor(context: BytecodePatchContext, layoutIds: Set<Int>) =
    methodReferences.any { reference ->
        reference.name == "<init>" && context.constructorLiteralValues(reference).any(layoutIds::contains)
    }

private fun BytecodePatchContext.constructorLiteralValues(
    reference: MethodReference,
    visitedConstructors: MutableSet<String> = mutableSetOf(),
): Set<Int> {
    val key = buildString {
        append(reference.definingClass)
        append("->")
        append(reference.name)
        append(reference.parameterTypes.joinToString(prefix = "(", postfix = ")") { it.toString() })
    }
    if (!visitedConstructors.add(key)) return emptySet()

    val constructor = classDefByOrNull(reference.definingClass)
        ?.methods
        ?.firstOrNull { method ->
            method.name == reference.name &&
                method.parameterTypes.map { it.toString() } == reference.parameterTypes.map { it.toString() } &&
                method.returnType == reference.returnType
        }
        ?: return emptySet()

    val delegatedConstructorValues = constructor.implementation?.instructions
        ?.mapNotNull { it.getReference<MethodReference>() }
        ?.filter { it.name == "<init>" }
        ?.flatMap { constructorLiteralValues(it, visitedConstructors) }
        ?.toSet()
        ?: emptySet()

    return constructor.literalValues() + delegatedConstructorValues
}

private fun Method.literalValues(): Set<Int> =
    implementation?.instructions
        ?.mapNotNull { it.narrowLiteralRegister()?.second }
        ?.toSet()
        ?: emptySet()

private fun MutableMethod.inferViewTypeBranches(): List<ItemTypeBranch> {
    val methodInstructions = instructions
    val instructionOffsets = methodInstructions.instructionOffsets()
    val viewTypeRegister = parameterRegister(1)
    val dispatchEndIndex = methodInstructions.indexOfFirstLayoutInflaterCall()
        .takeIf { it >= 0 }
        ?: methodInstructions.size
    val registerValues = mutableMapOf<Int, Int>()
    val branches = mutableListOf<ItemTypeBranch>()

    for (index in 0 until dispatchEndIndex) {
        val instruction = methodInstructions[index]
        instruction.narrowLiteralRegister()?.let { (register, value) ->
            registerValues[register] = value
        }

        if (instruction.opcode == Opcode.PACKED_SWITCH || instruction.opcode == Opcode.SPARSE_SWITCH) {
            if ((instruction as? OneRegisterInstruction)?.registerA != viewTypeRegister) continue

            val switchOffset = instructionOffsets[index]
            val payloadOffset = switchOffset + (instruction as OffsetInstruction).codeOffset
            val payloadIndex = instructionOffsets.indexOfFirst { it == payloadOffset }
            val payload = methodInstructions.getOrNull(payloadIndex) as? SwitchPayload ?: continue

            branches += payload.switchElements.mapNotNull { element ->
                val targetIndex = instructionOffsets.indexOfFirst { it == switchOffset + element.offset }
                if (targetIndex >= 0) ItemTypeBranch(element.key, targetIndex) else null
            }
            continue
        }

        if (instruction.opcode != Opcode.IF_EQ) continue

        val twoRegisterInstruction = instruction as? TwoRegisterInstruction ?: continue
        val comparedRegister = when (viewTypeRegister) {
            twoRegisterInstruction.registerA -> twoRegisterInstruction.registerB
            twoRegisterInstruction.registerB -> twoRegisterInstruction.registerA
            else -> continue
        }
        val type = registerValues[comparedRegister] ?: continue
        val targetOffset = instructionOffsets[index] + (instruction as OffsetInstruction).codeOffset
        val targetIndex = instructionOffsets.indexOfFirst { it == targetOffset }
        if (targetIndex >= 0) {
            branches += ItemTypeBranch(type, targetIndex)
        }
    }

    return branches
        .distinctBy { it.type }
        .ifEmpty { throw PatchException("Could not infer main item type branches") }
}

private fun MutableMethod.buildBranchBlocks(branches: List<ItemTypeBranch>): List<BranchBlock> {
    val methodInstructions = instructions
    val branchStarts = branches.map { it.startIndex }.distinct().sorted()
    val payloadStart = methodInstructions.indexOfFirst { it is SwitchPayload }
        .takeIf { it >= 0 }
        ?: methodInstructions.size

    return branches.distinctBy { it.type }.map { branch ->
        val endIndex = branchStarts.firstOrNull { it > branch.startIndex } ?: payloadStart
        BranchBlock(
            type = branch.type,
            instructions = methodInstructions.subList(branch.startIndex, endIndex),
        )
    }
}

private fun List<Instruction>.indexOfFirstLayoutInflaterCall() =
    indexOfFirst { instruction ->
        instruction.getReference<MethodReference>()?.let { reference ->
            reference.definingClass == "Landroid/view/LayoutInflater;" &&
                reference.name == "from"
        } == true
    }

private fun List<Instruction>.instructionOffsets(): IntArray {
    var offset = 0
    return IntArray(size) { index ->
        offset.also {
            offset += this[index].codeUnits
        }
    }
}

private fun MutableMethod.parameterRegister(parameterIndex: Int): Int {
    val implementation = implementation
        ?: throw PatchException("Could not inspect registers for $definingClass->$name")
    val parameterTypes = parameterTypes.map { it.toString() }
    if (parameterIndex !in parameterTypes.indices) {
        throw PatchException("Parameter $parameterIndex is not available in $definingClass->$name")
    }

    val receiverWidth = if (AccessFlags.STATIC.isSet(accessFlags)) 0 else 1
    val parameterWidth = parameterTypes.sumOf { it.registerWidth }
    val firstParameterRegister = implementation.registerCount - receiverWidth - parameterWidth + receiverWidth

    return firstParameterRegister + parameterTypes.take(parameterIndex).sumOf { it.registerWidth }
}

private val String.registerWidth: Int
    get() = if (this == "J" || this == "D") 2 else 1

private fun Instruction.narrowLiteralRegister(): Pair<Int, Int>? {
    val register = (this as? OneRegisterInstruction)?.registerA ?: return null
    val literal = (this as? NarrowLiteralInstruction)?.narrowLiteral ?: return null
    return register to literal
}

private fun String.toSmaliStringLiteral(): String =
    replace("\\", "\\\\").replace("\"", "\\\"")
