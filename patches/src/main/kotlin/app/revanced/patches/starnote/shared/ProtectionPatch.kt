package app.revanced.patches.starnote.shared

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.rawResourcePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
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
            method.addInstructions(
                instructionMatches.last().index - 4,
                """
                    const-string v0, "$LIBRARY_NAME"
                    invoke-static { v0 }, Ljava/lang/System;->loadLibrary(Ljava/lang/String;)V
                """,
            )
        }
    }
}