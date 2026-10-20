package app.revanced.patches.starnote.shared

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.util.findFreeRegister
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser

private const val LIBRARY_NAME = "starnote-patch"
private const val LIBRARY_PATH = "lib/arm64-v8a/lib$LIBRARY_NAME.so"
private const val PROTECTION_CLASS = "Lcom/sagittarius/v6/StubApplication;"
private const val STATUS_METHOD = "getStarnoteIntegrityPatchStatus"
private const val REPORT_METHOD = "reportStarnoteIntegrityPatchStatus"

private object ProtectionInitializerFingerprint : Fingerprint(
    definingClass = PROTECTION_CLASS,
    accessFlags = listOf(AccessFlags.PROTECTED),
    parameters = listOf("Landroid/content/Context;"),
    returnType = "V",
    filters = listOf(
        methodCall("Landroid/os/Debug;->isDebuggerConnected()Z"),
        methodCall("Landroid/os/Process;->killProcess(I)V"),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            parameters = listOf(
                "Landroid/content/Context;",
                "Ljava/lang/String;",
                "Ljava/lang/String;",
                "Ljava/lang/String;",
                "I",
            ),
            returnType = "V",
        ),
    ),
)

private val addProtectionLibraryPatch = rawResourcePatch {
    execute {
        val library = ::javaClass.javaClass.classLoader
            .getResourceAsStream("starnote/$LIBRARY_PATH")
            ?.use { it.readAllBytes() }
            ?: throw PatchException("Failed to load $LIBRARY_PATH")
        get(LIBRARY_PATH).apply {
            parentFile.mkdirs()
            writeBytes(library)
        }

        val uncompressedFiles = get("../uncompressed-files.json")
        val json = JsonParser.parseString(uncompressedFiles.readText()).asJsonObject
        json.getAsJsonArray("paths").add(LIBRARY_PATH)
        uncompressedFiles.writeText(GsonBuilder().setPrettyPrinting().create().toJson(json))
    }
}

internal val bypassProtectionIntegrityPatch = bytecodePatch {
    dependsOn(addProtectionLibraryPatch)

    execute {
        ProtectionInitializerFingerprint.apply {
            classDef.methods.add(
                ImmutableMethod(
                    PROTECTION_CLASS,
                    STATUS_METHOD,
                    emptyList(),
                    "I",
                    AccessFlags.PRIVATE.value or AccessFlags.STATIC.value or AccessFlags.NATIVE.value,
                    null,
                    null,
                    null,
                ).toMutable(),
            )
            classDef.methods.add(
                ImmutableMethod(
                    PROTECTION_CLASS,
                    REPORT_METHOD,
                    emptyList(),
                    "V",
                    AccessFlags.PRIVATE.value or AccessFlags.STATIC.value,
                    null,
                    null,
                    MutableMethodImplementation(2),
                ).toMutable().apply {
                    addInstructions(
                        0,
                        """
                            invoke-static {}, $PROTECTION_CLASS->$STATUS_METHOD()I
                            move-result v0
                            if-lez v0, :failed
                            const-string v0, "Integrity bypass active"
                            const-string v1, "StarNotePatch"
                            invoke-static {v1, v0}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I
                            return-void
                            :failed
                            invoke-static {v0}, Ljava/lang/Integer;->toString(I)Ljava/lang/String;
                            move-result-object v0
                            const-string v1, "Integrity bypass failed, status="
                            invoke-virtual {v1, v0}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;
                            move-result-object v0
                            const-string v1, "StarNotePatch"
                            invoke-static {v1, v0}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;)I
                            return-void
                        """,
                    )
                },
            )

            val nativeInitializationIndex = instructionMatches.last().index
            val nativeInitialization = method.getInstruction(nativeInitializationIndex) as? FiveRegisterInstruction
                ?: throw PatchException("Unsupported StarNote protection initializer invocation")
            val packageNameRegister = nativeInitialization.registerD
            val argumentLoadIndex = (0 until nativeInitializationIndex).lastOrNull { index ->
                val instruction = method.getInstruction(index)
                instruction.opcode == Opcode.SGET_OBJECT &&
                    (instruction as? OneRegisterInstruction)?.registerA == packageNameRegister &&
                    instruction.getReference<FieldReference>()?.type == "Ljava/lang/String;"
            } ?: throw PatchException("Could not find StarNote protection initializer argument load")
            val libraryRegister = method.findFreeRegister(argumentLoadIndex)
            method.addInstructions(
                argumentLoadIndex,
                """
                    const-string v$libraryRegister, "$LIBRARY_NAME"
                    invoke-static {v$libraryRegister}, Ljava/lang/System;->loadLibrary(Ljava/lang/String;)V
                    invoke-static {}, $PROTECTION_CLASS->$REPORT_METHOD()V
                """,
            )
        }
    }
}