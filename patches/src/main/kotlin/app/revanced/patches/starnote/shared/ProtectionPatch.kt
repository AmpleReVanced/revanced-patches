package app.revanced.patches.starnote.shared

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.util.findFreeRegister
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser

private const val LIBRARY_NAME = "starnote-patch"
private const val LIBRARY_PATH = "lib/arm64-v8a/lib$LIBRARY_NAME.so"

private object ProtectionInitializerFingerprint : Fingerprint(
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
                """,
            )
        }
    }
}