package app.revanced.patches.samsungkeyboard.misc.nononeui

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstructionOrNull
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.anyInstruction
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.ResourcePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.all.misc.resources.addResourcesPatch
import app.morphe.util.asSequence
import app.morphe.util.findInstructionIndicesReversed
import app.morphe.util.findMutableMethodOf
import app.morphe.util.getNode
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstStringInstruction
import app.morphe.util.indexOfFirstStringInstructionOrThrow
import app.morphe.util.registersUsed
import app.revanced.patches.samsungkeyboard.shared.Constants.COMPATIBILITY_SAMSUNG_KEYBOARD
import app.revanced.util.argumentRegister
import app.revanced.util.parameterTypeNames
import app.revanced.util.parameterRegister
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rc
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableFieldReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference
import org.w3c.dom.Element

private const val CONTEXT_TYPE = "Landroid/content/Context;"
private const val INPUT_METHOD_SERVICE_TYPE = "Landroid/inputmethodservice/InputMethodService;"
private const val EXTENSION_PACKAGE = "Lapp/revanced/extension/samsungkeyboard/"
private const val CLIPBOARD_COMPAT_TYPE = "${EXTENSION_PACKAGE}ClipboardCompat;"
private const val DEVICE_COMPAT_TYPE = "${EXTENSION_PACKAGE}DeviceCompat;"
private const val DRAWABLE_LOADER_TYPE = "${EXTENSION_PACKAGE}DrawableLoader;"
private const val FEEDBACK_COMPAT_TYPE = "${EXTENSION_PACKAGE}FeedbackCompat;"
private const val RESOURCE_LOADER_TYPE = "${EXTENSION_PACKAGE}ResourceLoader;"
private const val SETTINGS_STORE_TYPE = "${EXTENSION_PACKAGE}SettingsStore;"
private const val TOOLBAR_COMPAT_TYPE = "${EXTENSION_PACKAGE}ToolbarCompat;"
private const val WINDOW_COMPAT_TYPE = "${EXTENSION_PACKAGE}WindowCompat;"
private const val SPR_STYLEABLE_TYPE = "${EXTENSION_PACKAGE}SprStyleable;"
private const val SPR_PACKAGE = "Lcom/samsung/android/spr/"
private const val SAMSUNG_SPR_STYLEABLE_TYPE = "Lcom/samsung/android/spr/engine/R\$styleable;"
private const val SPR_DRAWABLE_CLASS = "com.samsung.android.spr.drawable.SprDrawable"
private const val SPR_RESOURCE_PREFIX = "revanced_spr_"
private const val GIF_ENCODER_PATH = "lib/arm64-v8a/libagifencoder.quram.so"
private const val SETTINGS_PACKAGE = "Lcom/samsung/android/honeyboard/settings/"
private const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
private lateinit var applicationClassType: String
private lateinit var inputMethodServiceClassTypes: List<String>

private data class ReplacementOptions(
    val clipboardService: Boolean,
    val drawableAccess: Boolean,
    val settingsWindowFlags: Boolean,
)

private val enableNonOneUiResourcesPatch = resourcePatch {
    compatibleWith(COMPATIBILITY_SAMSUNG_KEYBOARD)

    execute {
        document("AndroidManifest.xml").use { document ->
            val manifest = document.getNode("manifest") as Element
            val packageName = manifest.getAttribute("package")
            manifest.apply {
                removeAttribute("android:sharedUserId")
                removeAttribute("coreApp")
            }
            (document.getNode("uses-sdk") as Element).setAttribute("android:targetSdkVersion", "33")
            val application = document.getNode("application") as Element
            application.apply {
                removeAttribute("android:crossProfile")
                setAttribute("android:extractNativeLibs", "true")
                applicationClassType = getAttribute("android:name").toClassType(packageName)
            }
            inputMethodServiceClassTypes = document.getElementsByTagName("service")
                .asSequence()
                .filterIsInstance<Element>()
                .filter { it.getAttribute("android:permission") == "android.permission.BIND_INPUT_METHOD" }
                .map { it.getAttribute("android:name").toClassType(packageName) }
                .toList()
        }
        replaceSprResources()
        addGifEncoderLibrary()
    }
}

private fun ResourcePatchContext.addGifEncoderLibrary() {
    val destination = get(GIF_ENCODER_PATH).apply { parentFile.mkdirs() }
    val source = ::javaClass.javaClass.classLoader.getResourceAsStream("samsungkeyboard/$GIF_ENCODER_PATH")
        ?: throw PatchException("Could not load ${destination.name}.")
    source.use { input ->
        destination.outputStream().use { output -> input.copyTo(output) }
    }
}

private fun ResourcePatchContext.replaceSprResources() {
    val resourceDirectory = get("res")
    val drawableFiles = resourceDirectory.listFiles()
        ?.asSequence()
        ?.filter { it.isDirectory && it.name.startsWith("drawable") }
        ?.flatMap { it.listFiles()?.asSequence() ?: emptySequence() }
        ?.filter { it.isFile }
        ?.toList()
        .orEmpty()
    val sprFiles = drawableFiles.filter { it.isSpr() }
    val resourceNames = sprFiles.mapTo(HashSet(sprFiles.size)) { it.nameWithoutExtension }

    drawableFiles.asSequence()
        .filter { it.extension == "xml" && "<bitmap" in it.readText() }
        .forEach { file ->
            document(file.relativeTo(resourceDirectory.parentFile).path).use { document ->
                document.getElementsByTagName("bitmap")
                    .asSequence()
                    .filterIsInstance<Element>()
                    .mapNotNull { element ->
                        element.getAttribute("android:src").drawableName()
                            ?.takeIf(resourceNames::contains)
                            ?.let { element to it }
                    }
                    .toList()
                    .forEach { (element, name) ->
                        element.setAttribute("android:src", "@drawable/$SPR_RESOURCE_PREFIX$name")
                        document.renameNode(element, null, SPR_DRAWABLE_CLASS)
                    }
            }
        }

    sprFiles.forEach { source ->
        val name = source.nameWithoutExtension
        val backup = source.parentFile.resolve("$SPR_RESOURCE_PREFIX${source.name}")
        val wrapper = source.parentFile.resolve("$name.xml")
        if (backup.exists() || wrapper.exists() || !source.renameTo(backup)) {
            throw PatchException("Could not relocate SPR resource ${source.absolutePath}.")
        }
        wrapper.writeText(
            """
                <?xml version="1.0" encoding="utf-8"?>
                <$SPR_DRAWABLE_CLASS xmlns:android="$ANDROID_NAMESPACE"
                    android:src="@drawable/$SPR_RESOURCE_PREFIX$name" />
            """.trimIndent(),
        )
    }
}

private fun java.io.File.isSpr() = inputStream().use { input ->
    val header = ByteArray(4)
    input.read(header) == header.size && header.contentEquals(byteArrayOf(0x53, 0x50, 0x52, 0x00))
}

private fun String.drawableName() = substringAfter("@drawable/", "").takeIf(String::isNotEmpty)

@Suppress("unused")
val enableNonOneUiPatch = bytecodePatch(
    name = "Enable non-One UI ROM support",
    description = "Makes Samsung Keyboard installable and usable on non-One UI ROMs.",
) {
    compatibleWith(COMPATIBILITY_SAMSUNG_KEYBOARD)
    dependsOn(
        addResourcesPatch,
        addFeedbackSettingsPatch,
        enableNonOneUiResourcesPatch,
    )
    extendWith("extensions/samsungkeyboard.mpe")

    execute {
        patchPlatformDependencies()
        patchGifVisibility()

        StoreDownloadRequestFingerprint.method.applyStoreProfile()
        StoreUpdateCheckRequestFingerprint.method.applyStoreProfile()
        ShowSoftInputFingerprint.method.applyShowSoftInputCompat()
        initializeInputMethodServices()
        initializeApplication()
    }
}

private fun BytecodePatchContext.patchGifVisibility() {
    val match = GifVisibilityFingerprint.instructionMatches.last()
    val visibilityRegister = match.getInstruction<Instruction>().argumentRegister(0)
    GifVisibilityFingerprint.method.addInstructions(
        match.index,
        """
            invoke-static {v$visibilityRegister}, $TOOLBAR_COMPAT_TYPE->gifVisibility(I)I
            move-result v$visibilityRegister
        """.trimIndent(),
    )
}

private fun BytecodePatchContext.patchPlatformDependencies() {
    val toolbarFeatureAccess = anyInstruction(
        ClipboardFeatureFlagFingerprint.featureFlagAccess(),
        GifFeatureFlagFingerprint.featureFlagAccess(),
    )
    val definedTypes = buildSet { classDefForEach { add(it.type) } }
    val unavailableType: (String) -> Boolean = { it.isUnavailableSamsungType(definedTypes) }

    classDefForEach classLoop@{ classDef ->
        if (classDef.type.startsWith(EXTENSION_PACKAGE)) return@classLoop

        val replaceDrawableAccess = !classDef.type.startsWith(SPR_PACKAGE)
        val replaceSettingsWindowFlags = classDef.type.startsWith(SETTINGS_PACKAGE)
        val mutableClass by lazy { mutableClassDefBy(classDef) }

        if (classDef.interfaces.any(unavailableType)) {
            mutableClass.interfaces.removeAll(unavailableType)
        }
        classDef.methods.forEach methodLoop@{ method ->
            if (method.implementation == null) return@methodLoop

            val mutableMethod by lazy { mutableClass.findMutableMethodOf(method) }
            method.findInstructionIndicesReversed(toolbarFeatureAccess).forEach { index ->
                mutableMethod.replaceWithTrue(index)
            }
            val options = ReplacementOptions(
                clipboardService = method.indexOfFirstStringInstruction("semclipboard") >= 0,
                drawableAccess = replaceDrawableAccess,
                settingsWindowFlags = replaceSettingsWindowFlags,
            )
            method.findInstructionIndicesReversed {
                requiresPlatformReplacement(unavailableType, options)
            }.forEach { index ->
                mutableMethod.replacePlatformDependency(index, unavailableType, options)
            }
            if (method.isProviderCallerVerification()) mutableMethod.allowSelfProviderAccess()
        }
    }
}

context(_: BytecodePatchContext)
private fun Fingerprint.featureFlagAccess() = fieldAccess(
    instructionMatches.first().getInstruction<ReferenceInstruction>().reference as FieldReference,
    Opcode.SGET_BOOLEAN,
)

private fun BytecodePatchContext.initializeInputMethodServices() {
    val initialized = inputMethodServiceClassTypes.count { type ->
        val classDef = classDefBy(type)
        val onCreate = classDef.methods.firstOrNull { method ->
            method.name == "onCreate" && method.parameterTypeNames.isEmpty() && method.returnType == "V"
        } ?: return@count false
        mutableClassDefBy(classDef).findMutableMethodOf(onCreate).addInstruction(
            0,
            "invoke-static/range {p0 .. p0}, $WINDOW_COMPAT_TYPE->initialize($INPUT_METHOD_SERVICE_TYPE)V",
        )
        true
    }
    if (initialized == 0) throw PatchException("Could not find an input method service initializer.")
}

private fun BytecodePatchContext.initializeApplication() {
    val applicationClass = classDefBy(applicationClassType)
    val attachBaseContext = applicationClass.methods.firstOrNull { method ->
        method.name == "attachBaseContext" &&
            method.parameterTypeNames == listOf(CONTEXT_TYPE) &&
            method.returnType == "V"
    }
    val initializeMethod = attachBaseContext ?: applicationClass.methods.firstOrNull { method ->
        method.name == "onCreate" && method.parameterTypeNames.isEmpty() && method.returnType == "V"
    } ?: throw PatchException("Could not find the application initialization method.")
    val contextRegister = if (initializeMethod === attachBaseContext) "p1" else "p0"
    mutableClassDefBy(applicationClass).findMutableMethodOf(initializeMethod).addInstructions(
        0,
        """
            invoke-static {$contextRegister}, $TOOLBAR_COMPAT_TYPE->initialize($CONTEXT_TYPE)V
            invoke-static {$contextRegister}, $SETTINGS_STORE_TYPE->initialize($CONTEXT_TYPE)V
        """.trimIndent(),
    )
}

private fun MutableMethod.replaceWithTrue(index: Int) {
    val register = getInstruction<OneRegisterInstruction>(index).registerA
    replaceInstruction(index, "const/4 v$register, 0x1")
}

private fun MutableMethod.applyShowSoftInputCompat() {
    val flagsRegister = parameterRegister(0)
    val contextRegister = parameterRegister(1)
    addInstructions(
        0,
        """
            invoke-static {v$contextRegister, v$flagsRegister}, $WINDOW_COMPAT_TYPE->showSoftInput(${CONTEXT_TYPE}I)V
            return-void
        """.trimIndent(),
    )
}

private fun MutableMethod.allowSelfProviderAccess() = addInstructionsWithLabels(
    0,
    """
        invoke-static {}, Landroid/os/Binder;->getCallingUid()I
        move-result v0
        invoke-static {}, Landroid/os/Process;->myUid()I
        move-result v1
        if-eq v0, v1, :allowed
        invoke-virtual {p0}, Landroid/content/Context;->getPackageName()Ljava/lang/String;
        move-result-object v0
        invoke-virtual {v0, p1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
        move-result v0
        if-eqz v0, :original
        :allowed
        const/4 v0, 0x1
        return v0
        :original
        nop
    """.trimIndent(),
)

private fun Method.isProviderCallerVerification() =
    returnType == "Z" &&
        parameterTypeNames == listOf(CONTEXT_TYPE, "Ljava/lang/String;") &&
        indexOfFirstStringInstruction("valid signature : ") >= 0 &&
        indexOfFirstStringInstruction("certification failed") >= 0

private fun Instruction.requiresPlatformReplacement(
    unavailableType: (String) -> Boolean,
    options: ReplacementOptions,
): Boolean {
    val reference = (this as? ReferenceInstruction)?.reference ?: return false
    if (reference is MethodReference &&
        (reference.isSetInputViewReference() ||
            reference.compatReference(options, opcode) != null)
    ) return true
    if (reference is FieldReference && reference.sprStyleableReference() != null) return true

    return usesUnavailableOneUiReference(unavailableType)
}

private fun MutableMethod.applyStoreProfile() {
    replaceStoreRequestValue("&deviceId=", "getStoreModel")
    replaceStoreRequestValue("&oneUiVersion=", "getStoreOneUiVersion")
}

private fun MutableMethod.replaceStoreRequestValue(key: String, extensionMethod: String) {
    val keyIndex = indexOfFirstStringInstructionOrThrow(key)
    val valueIndex = indexOfFirstInstructionOrThrow(keyIndex) {
        (opcode == Opcode.INVOKE_STATIC || opcode == Opcode.INVOKE_STATIC_RANGE) &&
            getReference<MethodReference>()?.let { reference ->
                reference.parameterTypeNames.isEmpty() && reference.returnType == "Ljava/lang/String;"
            } == true
    }
    val resultIndex = valueIndex + 1
    val resultInstruction = getInstruction<OneRegisterInstruction>(resultIndex)
    if (resultInstruction.opcode != Opcode.MOVE_RESULT_OBJECT) {
        throw PatchException("Could not find the store request value result.")
    }
    val register = resultInstruction.registerA
    addInstructions(
        resultIndex + 1,
        """
            invoke-static {v$register}, $DEVICE_COMPAT_TYPE->$extensionMethod(Ljava/lang/String;)Ljava/lang/String;
            move-result-object v$register
        """,
    )
}

private fun MutableMethod.replacePlatformDependency(
    index: Int,
    unavailableType: (String) -> Boolean,
    options: ReplacementOptions,
) {
    val instruction = getInstructionOrNull<Instruction>(index) ?: return
    val reference = (instruction as? ReferenceInstruction)?.reference ?: return
    if (reference is MethodReference) {
        if (reference.isSetInputViewReference()) {
            val register = instruction.argumentRegister(0)
            addInstruction(
                index,
                "invoke-static/range {v$register .. v$register}, $WINDOW_COMPAT_TYPE->captureInputView(Landroid/view/View;)V",
            )
            return
        }
        val replacement = reference.compatReference(options, instruction.opcode)
        if (replacement != null) {
            replaceInvokeReference(index, instruction, replacement)
            return
        }
    }
    if (reference is FieldReference) {
        val replacement = reference.sprStyleableReference()
        if (replacement != null) {
            replaceFieldReference(index, instruction, replacement)
            return
        }
    }
    if (!instruction.usesUnavailableOneUiReference(unavailableType)) return

    when (reference) {
        is MethodReference -> replaceMethodCall(index, reference)
        is FieldReference -> replaceFieldAccess(index, reference.type)
        is TypeReference -> replaceTypeReference(index, instruction.opcode)
    }
}

private fun FieldReference.sprStyleableReference() =
    takeIf {
        definingClass == SAMSUNG_SPR_STYLEABLE_TYPE &&
            (name == "SprDrawable" || name.startsWith("SprDrawable_"))
    }?.let { ImmutableFieldReference(SPR_STYLEABLE_TYPE, name, type) }

private fun MutableMethod.replaceFieldReference(
    index: Int,
    instruction: Instruction,
    reference: FieldReference,
) {
    val register = (instruction as? OneRegisterInstruction)?.registerA
        ?: throw PatchException("Unsupported field access instruction.")
    replaceInstruction(index, BuilderInstruction21c(instruction.opcode, register, reference))
}

private fun MethodReference.isSetInputViewReference() =
    matches(
        INPUT_METHOD_SERVICE_TYPE,
        "setInputView",
        listOf("Landroid/view/View;"),
        "V",
    )

private fun MethodReference.compatReference(
    options: ReplacementOptions,
    opcode: Opcode,
) = windowCompatReference(options.settingsWindowFlags, opcode)
    ?: clipboardCompatReference().takeIf { options.clipboardService }
    ?: deviceCompatReference()
    ?: feedbackCompatReference()
    ?: resourceLoaderReference()
    ?: drawableLoaderReference().takeIf { options.drawableAccess }
    ?: settingsStoreReference()

private fun MethodReference.clipboardCompatReference() =
    if (matches(CONTEXT_TYPE, "getSystemService", listOf("Ljava/lang/String;"), "Ljava/lang/Object;")) {
        toExtensionReference(CLIPBOARD_COMPAT_TYPE)
    } else {
        null
    }

private fun MethodReference.windowCompatReference(
    replaceSettingsWindowFlags: Boolean,
    opcode: Opcode,
): MethodReference? {
    val supported = when {
        matches("Landroid/app/Activity;", "semOverridePendingTransition", listOf("I", "I"), "V") -> true
        matches("Landroid/app/Dialog;", "show", emptyList(), "V") -> !opcode.isSuperInvoke
        matches(
            "Landroid/view/ViewManager;",
            "addView",
            listOf("Landroid/view/View;", "Landroid/view/ViewGroup\$LayoutParams;"),
            "V",
        ) -> true
        matches("Landroid/view/Window;", "setType", listOf("I"), "V") -> true
        replaceSettingsWindowFlags &&
            matches("Landroid/view/Window;", "setFlags", listOf("I", "I"), "V") -> true
        else -> false
    }
    return if (supported) toExtensionReference(WINDOW_COMPAT_TYPE) else null
}

private fun MethodReference.matches(
    definingClass: String,
    name: String,
    parameters: List<String>,
    returnType: String,
) = this.definingClass == definingClass &&
    this.name == name &&
    parameterTypeNames == parameters &&
    this.returnType == returnType

private fun MethodReference.toExtensionReference(
    extensionType: String,
    name: String = this.name,
    parameters: List<String> = listOf(definingClass) + parameterTypeNames,
) = ImmutableMethodReference(extensionType, name, parameters, returnType)

private fun MutableMethod.replaceInvokeReference(
    index: Int,
    instruction: Instruction,
    reference: MethodReference,
) {
    val opcode = when (instruction) {
        is RegisterRangeInstruction -> Opcode.INVOKE_STATIC_RANGE
        else -> Opcode.INVOKE_STATIC
    }
    val replacement = when (instruction) {
        is FiveRegisterInstruction -> BuilderInstruction35c(
            opcode,
            instruction.registerCount,
            instruction.registerC,
            instruction.registerD,
            instruction.registerE,
            instruction.registerF,
            instruction.registerG,
            reference,
        )
        is RegisterRangeInstruction -> BuilderInstruction3rc(
            opcode,
            instruction.startRegister,
            instruction.registerCount,
            reference,
        )
        else -> throw PatchException("Unsupported invocation instruction.")
    }
    replaceInstruction(index, replacement)
}

private fun MethodReference.deviceCompatReference(): MethodReference? {
    if (!matches("Landroid/os/Build;", "getSerial", emptyList(), "Ljava/lang/String;")) return null
    return toExtensionReference(DEVICE_COMPAT_TYPE, parameters = emptyList())
}

private fun MethodReference.feedbackCompatReference(): MethodReference? {
    val parameters = parameterTypeNames
    val replacementParameters = when {
        matches("Landroid/media/AudioManager;", "semGetSituationVolume", listOf("I", "I"), "F") ->
            listOf(definingClass) + parameters
        matches("Landroid/os/Vibrator;", "semGetSupportedVibrationType", emptyList(), "I") ->
            listOf(definingClass)
        matches("Landroid/view/HapticFeedbackConstants;", "semGetVibrationIndex", listOf("I"), "I") ->
            parameters
        matches(
            "Landroid/os/VibrationEffect;",
            "semCreateWaveform",
            listOf("I", "I", "Landroid/os/VibrationEffect\$SemMagnitudeType;"),
            "Landroid/os/VibrationEffect;",
        ) -> listOf("I", "I", "Ljava/lang/Object;")
        else -> return null
    }
    return toExtensionReference(FEEDBACK_COMPAT_TYPE, parameters = replacementParameters)
}

private fun MethodReference.resourceLoaderReference() =
    if (matches("Landroid/content/res/Resources;", "getDimensionPixelSize", listOf("I"), "I")) {
        toExtensionReference(RESOURCE_LOADER_TYPE)
    } else {
        null
    }

private fun MethodReference.drawableLoaderReference(): MethodReference? {
    val parameters = parameterTypeNames
    val replacementParameters = when {
        matches(
            CONTEXT_TYPE,
            "getDrawable",
            listOf("I"),
            "Landroid/graphics/drawable/Drawable;",
        ) -> listOf(definingClass) + parameters
        definingClass == "Landroid/content/res/Resources;" &&
            name == "getDrawable" &&
            parameters in drawableResourceParameters &&
            returnType == "Landroid/graphics/drawable/Drawable;" -> listOf(definingClass) + parameters
        matches(
            "Landroid/content/res/TypedArray;",
            "getDrawable",
            listOf("I"),
            "Landroid/graphics/drawable/Drawable;",
        ) -> listOf(definingClass) + parameters
        matches(
            "Landroid/graphics/BitmapFactory;",
            "decodeResource",
            listOf("Landroid/content/res/Resources;", "I"),
            "Landroid/graphics/Bitmap;",
        ) -> parameters
        else -> return null
    }
    return toExtensionReference(DRAWABLE_LOADER_TYPE, parameters = replacementParameters)
}

private fun MutableMethod.replaceMethodCall(index: Int, reference: MethodReference) {
    val instruction = getInstructionOrNull<Instruction>(index) ?: return
    if (reference.name == "<init>") {
        replaceConstructorCall(index, instruction, reference)
        return
    }

    replaceInstruction(index, "nop")
    if (reference.returnType == "V") return

    val result = (getInstructionOrNull<Instruction>(index + 1) as? OneRegisterInstruction)
        ?.takeIf { it.opcode.isMoveResult }
        ?: return
    val resultRegister = result.registerA

    if (!instruction.opcode.isStaticInvoke && reference.returnType == reference.definingClass) {
        val receiverRegister = instruction.receiverRegister() ?: return
        replaceInstruction(index + 1, "move-object/from16 v$resultRegister, v$receiverRegister")
        return
    }

    reference.returnType.collectionDefault()?.let { defaultCall ->
        replaceInstruction(index, defaultCall)
        return
    }

    replaceInstruction(index + 1, defaultValue(reference.returnType, resultRegister))
    if (reference.returnType.isObjectType) removeKotlinNullChecks(index + 2, index + 5, resultRegister)
}

private fun MutableMethod.replaceConstructorCall(
    index: Int,
    instruction: Instruction,
    reference: MethodReference,
) {
    val receiver = instruction.receiverRegister()
        ?: throw PatchException("Could not resolve an unsupported constructor receiver.")
    val allocationIndex = (index - 1 downTo 0).firstOrNull { candidateIndex ->
        getInstructionOrNull<Instruction>(candidateIndex)?.let { candidate ->
            candidate.opcode == Opcode.NEW_INSTANCE &&
                (candidate as OneRegisterInstruction).registerA == receiver &&
                candidate.getReference<TypeReference>()?.type == reference.definingClass
        } == true
    } ?: throw PatchException("Could not resolve an unsupported constructor allocation in $definingClass->$name.")

    replaceInstruction(allocationIndex, "const/16 v$receiver, 0x0")
    replaceInstruction(index, "nop")
}

private fun MutableMethod.replaceFieldAccess(index: Int, type: String) {
    val instruction = getInstructionOrNull<Instruction>(index) ?: return
    if (!instruction.opcode.setsRegister()) {
        replaceInstruction(index, "nop")
        return
    }

    val register = instruction.registersUsed.firstOrNull()
        ?: throw PatchException("Unsupported field access instruction.")
    replaceInstruction(index, defaultValue(type, register))
    if (type.isObjectType) removeKotlinNullChecks(index + 1, index + 4, register)
}

private fun MutableMethod.replaceTypeReference(index: Int, opcode: Opcode) {
    when (opcode) {
        Opcode.CHECK_CAST -> {
            val register = (getInstructionOrNull<Instruction>(index) as? OneRegisterInstruction)?.registerA ?: return
            removeKotlinNullChecks(index - 4, index - 1, register)
            replaceInstruction(index, "nop")
        }
        Opcode.CONST_CLASS -> {
            val register = (getInstructionOrNull<Instruction>(index) as? OneRegisterInstruction)?.registerA ?: return
            replaceInstruction(index, "const-class v$register, Ljava/lang/Object;")
        }
        Opcode.FILLED_NEW_ARRAY,
        Opcode.FILLED_NEW_ARRAY_RANGE,
        -> {
            replaceInstruction(index, "nop")
            val result = (getInstructionOrNull<Instruction>(index + 1) as? OneRegisterInstruction)
                ?.takeIf { it.opcode == Opcode.MOVE_RESULT_OBJECT }
                ?: return
            replaceInstruction(index + 1, "const/16 v${result.registerA}, 0x0")
        }
        else -> {
            val instruction = getInstructionOrNull<Instruction>(index) ?: return
            if (!instruction.opcode.setsRegister()) {
                replaceInstruction(index, "nop")
                return
            }
            val register = instruction.registersUsed.firstOrNull()
                ?: throw PatchException("Unsupported type instruction.")
            replaceInstruction(index, defaultValue("Ljava/lang/Object;", register))
        }
    }
}

private fun MutableMethod.removeKotlinNullChecks(startIndex: Int, endIndex: Int, register: Int) {
    val implementation = implementation ?: return
    (startIndex.coerceAtLeast(0)..endIndex.coerceAtMost(implementation.instructions.lastIndex))
        .filter { index -> getInstructionOrNull<Instruction>(index)?.isKotlinNullCheck(register) == true }
        .forEach { index -> replaceInstruction(index, "nop") }
}

private fun Instruction.isKotlinNullCheck(register: Int): Boolean {
    val reference = (this as? ReferenceInstruction)?.reference as? MethodReference ?: return false
    if (reference.definingClass != "Lkotlin/jvm/internal/Intrinsics;" ||
        !reference.name.startsWith("checkNotNull")
    ) return false

    return runCatching { argumentRegister(0) }.getOrNull() == register
}

private fun Instruction.usesUnavailableOneUiReference(unavailableType: (String) -> Boolean): Boolean {
    val reference = (this as? ReferenceInstruction)?.reference ?: return false
    return when (reference) {
        is MethodReference ->
            unavailableType(reference.definingClass) ||
                reference.returnType.let(unavailableType) ||
                reference.parameterTypeNames.any(unavailableType) ||
                reference.isUnavailableSamsungFrameworkMethod()
        is FieldReference ->
            unavailableType(reference.definingClass) ||
                unavailableType(reference.type) ||
                reference.definingClass.startsWith("Landroid/") &&
                (reference.name.startsWith("sem") || reference.name.startsWith("SEM_"))
        is TypeReference -> unavailableType(reference.type)
        else -> false
    }
}

private fun MethodReference.isUnavailableSamsungFrameworkMethod() =
    definingClass.startsWith("Landroid/") &&
        (name.startsWith("sem") ||
            name.endsWith("AsUser") ||
            matches("Landroid/view/View;", "setHoverPopupType", listOf("I"), "V"))

private fun MethodReference.settingsStoreReference(): MethodReference? {
    val scope = when (definingClass) {
        "Landroid/provider/Settings\$Secure;" -> "secure"
        "Landroid/provider/Settings\$System;" -> "system"
        "Landroid/provider/Settings\$Global;" -> "global"
        else -> return null
    }
    val signature = "$name(${parameterTypeNames.joinToString("")})$returnType"
    if (signature !in supportedSettingsSignatures) return null

    return toExtensionReference(
        SETTINGS_STORE_TYPE,
        name = scope + name.replaceFirstChar { it.uppercaseChar() },
        parameters = parameterTypeNames,
    )
}

private fun String.isUnavailableSamsungType(definedTypes: Set<String>): Boolean {
    val type = dropWhile { it == '[' }
    if ((type.startsWith("Lcom/samsung/") || type.startsWith("Lcom/sec/")) && type !in definedTypes) return true
    if (!type.startsWith("Landroid/")) return false

    val simpleNames = type.substringAfterLast('/').removeSuffix(";").split('$')
    val outerName = simpleNames.first()
    return simpleNames.any { it.startsWith("Sem") } ||
        outerName == "DVFSHelper" ||
        outerName == "HoverPopupWindow"
}

private fun String.toClassType(packageName: String): String {
    val className = when {
        startsWith('.') -> packageName + this
        contains('.') -> this
        else -> "$packageName.$this"
    }
    return "L${className.replace('.', '/')};"
}

private fun Instruction.receiverRegister() = when (this) {
    is FiveRegisterInstruction -> registerC
    is RegisterRangeInstruction -> startRegister
    else -> null
}

private fun String.collectionDefault() = when (this) {
    "Ljava/lang/Iterable;",
    "Ljava/util/Collection;",
    "Ljava/util/List;",
    -> "invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;"
    "Ljava/util/Set;" -> "invoke-static {}, Ljava/util/Collections;->emptySet()Ljava/util/Set;"
    "Ljava/util/Map;" -> "invoke-static {}, Ljava/util/Collections;->emptyMap()Ljava/util/Map;"
    else -> null
}

private val String.isObjectType
    get() = startsWith('L') || startsWith('[')

private val Opcode.isMoveResult
    get() = this == Opcode.MOVE_RESULT || this == Opcode.MOVE_RESULT_WIDE || this == Opcode.MOVE_RESULT_OBJECT

private val Opcode.isStaticInvoke
    get() = this == Opcode.INVOKE_STATIC || this == Opcode.INVOKE_STATIC_RANGE

private val Opcode.isSuperInvoke
    get() = this == Opcode.INVOKE_SUPER || this == Opcode.INVOKE_SUPER_RANGE

private fun defaultValue(type: String, register: Int) = when (type) {
    "J", "D" -> "const-wide/16 v$register, 0x0"
    "Ljava/lang/String;", "Ljava/lang/CharSequence;" -> "const-string v$register, \"\""
    else -> "const/16 v$register, 0x0"
}

private val supportedSettingsSignatures = setOf(
    "getString(Landroid/content/ContentResolver;Ljava/lang/String;)Ljava/lang/String;",
    "getInt(Landroid/content/ContentResolver;Ljava/lang/String;)I",
    "getInt(Landroid/content/ContentResolver;Ljava/lang/String;I)I",
    "getFloat(Landroid/content/ContentResolver;Ljava/lang/String;F)F",
    "putString(Landroid/content/ContentResolver;Ljava/lang/String;Ljava/lang/String;)Z",
    "putInt(Landroid/content/ContentResolver;Ljava/lang/String;I)Z",
)

private val drawableResourceParameters = setOf(
    listOf("I"),
    listOf("I", "Landroid/content/res/Resources\$Theme;"),
)
