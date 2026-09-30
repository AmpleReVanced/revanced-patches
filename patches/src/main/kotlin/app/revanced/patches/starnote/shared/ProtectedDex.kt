package app.revanced.patches.starnote.shared

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.rawResourcePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.dexbacked.instruction.DexBackedInstruction
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.zip.Adler32
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.min

internal data class MethodCallFingerprint(
    val definingClass: String? = null,
    val name: String? = null,
    val parameters: List<String>? = null,
    val returnType: String? = null,
) {
    fun matches(reference: MethodReference) =
        (definingClass == null || reference.definingClass == definingClass) &&
            (name == null || reference.name == name) &&
            (parameters == null || reference.parameterTypes == parameters) &&
            (returnType == null || reference.returnType == returnType)
}

internal class ProtectedDexFingerprint(
    private val id: String,
    private val name: String? = null,
    private val accessFlags: List<AccessFlags> = emptyList(),
    private val parameters: List<String>? = null,
    private val returnType: String? = null,
    private val strings: List<String> = emptyList(),
    private val methodCalls: List<MethodCallFingerprint> = emptyList(),
) {
    fun matches(method: Method): Boolean {
        if ((name != null && method.name != name) ||
            accessFlags.any { !it.isSet(method.accessFlags) } ||
            parameters?.let { method.parameterTypes != it } == true ||
            returnType?.let { method.returnType != it } == true
        ) {
            return false
        }

        val instructions = method.implementation?.instructions ?: return false
        var callIndex = 0
        val foundStrings = mutableSetOf<String>()
        instructions.forEach { instruction ->
            val reference = (instruction as? ReferenceInstruction)?.reference ?: return@forEach
            if (reference is StringReference && reference.string in strings) {
                foundStrings.add(reference.string)
            }
            if (callIndex < methodCalls.size &&
                reference is MethodReference &&
                methodCalls[callIndex].matches(reference)
            ) {
                callIndex++
            }
        }

        return foundStrings.containsAll(strings) && callIndex == methodCalls.size
    }

    override fun toString() = id
}

internal object ProtectedDex {
    private val salt = "83d4e5534f4e330e".toByteArray()
    private val info = "97f3a59f2ae61f3a".toByteArray()
    private val dexMagic = "dex\n".toByteArray()
    private val metadataMagic = "md01".toByteArray()
    private lateinit var key: ByteArray
    private val payloads = mutableListOf<Payload>()

    private data class Payload(
        val file: File,
        val bytes: ByteArray,
        var modified: Boolean = false,
    )

    data class Match(
        val definingClass: String,
    )

    fun load(assets: File) {
        payloads.clear()
        val metadataFile = assets.resolve("baiduprotect.md")
        val metadata = crypt(
            metadataFile.readBytes(),
            "assets/baiduprotect.md".toByteArray(),
            metadataFile.length().toInt(),
        )
        if (!metadata.startsWith(metadataMagic)) {
            throw PatchException("Unsupported StarNote protection metadata")
        }

        val values = ByteBuffer.wrap(metadata).order(ByteOrder.LITTLE_ENDIAN)
        val count = values.getInt(0x44)
        key = metadata.copyOfRange(0x14, 0x24)
        repeat(count) { index ->
            val file = assets.resolve("baiduprotect${index + 1}.jar")
            val encrypted = file.readBytes()
            val bytes = inflate(crypt(encrypted, key, min(256, encrypted.size)))
            val expectedSize = values.getInt(0x48 + index * Int.SIZE_BYTES)
            if (bytes.size != expectedSize || !bytes.startsWith(dexMagic)) {
                throw PatchException("Invalid protected DEX ${index + 1}")
            }
            payloads += Payload(file, bytes)
        }
    }

    fun returnEarly(
        fingerprint: ProtectedDexFingerprint,
        value: Boolean,
        definingClass: String? = null,
    ): Match = patch(fingerprint, definingClass) { bytes, method ->
        patchReturn(bytes, method, value)
    }

    fun returnLong(
        fingerprint: ProtectedDexFingerprint,
        value: Long,
        definingClass: String? = null,
    ): Match = patch(fingerprint, definingClass) { bytes, method ->
        patchLongReturn(bytes, method, value)
    }

    private fun patch(
        fingerprint: ProtectedDexFingerprint,
        definingClass: String?,
        edit: (ByteArray, Method) -> Unit,
    ): Match {
        val matches = payloads.asSequence().flatMap { payload ->
            val dexFile = DexBackedDexFile(null, ByteBuffer.wrap(payload.bytes))
            dexFile.classes.asSequence()
                .filter { definingClass == null || it.type == definingClass }
                .flatMap { classDef ->
                    classDef.methods.asSequence()
                        .filter(fingerprint::matches)
                        .map { method -> Triple(payload, classDef, method) }
                }
        }.toList()
        if (matches.size != 1) {
            throw PatchException("Expected one $fingerprint match, found ${matches.size}")
        }

        val (payload, classDef, method) = matches.single()
        edit(payload.bytes, method)
        updateHeader(payload.bytes)
        payload.modified = true
        return Match(classDef.type)
    }

    fun write() {
        payloads.filter(Payload::modified).forEach { payload ->
            val compressed = deflate(payload.bytes)
            val encrypted = crypt(compressed, key, min(256, compressed.size))
            payload.file.writeBytes(encrypted)
        }
        payloads.clear()
    }

    private fun patchReturn(bytes: ByteArray, method: Method, value: Boolean) {
        overwriteReturn(bytes, method, byteArrayOf(0x12, (if (value) 0x10 else 0x00).toByte(), 0x0f, 0x00), 2, "Z", 1)
    }

    private fun patchLongReturn(bytes: ByteArray, method: Method, value: Long) {
        if (value !in Int.MIN_VALUE..Int.MAX_VALUE) {
            throw PatchException("Unsupported long return in $method")
        }
        val instructions = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
            .put(0x17.toByte())
            .put(0.toByte())
            .putInt(value.toInt())
            .put(0x10.toByte())
            .put(0.toByte())
            .array()
        overwriteReturn(bytes, method, instructions, 6, "J", 2)
    }

    private fun overwriteReturn(
        bytes: ByteArray,
        method: Method,
        replacement: ByteArray,
        coveredCodeUnits: Int,
        returnType: String,
        requiredRegisters: Int,
    ) {
        val implementation = method.implementation
            ?: throw PatchException("$method has no implementation")
        if (method.returnType != returnType || implementation.registerCount < requiredRegisters) {
            throw PatchException("Unsupported return in $method")
        }
        val instructions = implementation.instructions.iterator()
        val first = (if (instructions.hasNext()) instructions.next() else null) as? DexBackedInstruction
            ?: throw PatchException("$method has no instructions")
        var replacedCodeUnits = first.codeUnits
        while (replacedCodeUnits < coveredCodeUnits && instructions.hasNext()) {
            val instruction = instructions.next() as? DexBackedInstruction
                ?: throw PatchException("Unsupported instruction in $method")
            replacedCodeUnits += instruction.codeUnits
        }
        if (replacedCodeUnits < coveredCodeUnits || replacement.size > replacedCodeUnits * Short.SIZE_BYTES) {
            throw PatchException("$method is too short to patch")
        }
        val offset = first.instructionStart
        replacement.copyInto(bytes, offset)
        bytes.fill(0.toByte(), offset + replacement.size, offset + replacedCodeUnits * Short.SIZE_BYTES)
    }

    private fun updateHeader(bytes: ByteArray) {
        val signature = MessageDigest.getInstance("SHA-1").digest(bytes.copyOfRange(32, bytes.size))
        signature.copyInto(bytes, 12)
        val checksum = Adler32().apply {
            update(bytes, 12, bytes.size - 12)
        }.value.toInt()
        repeat(Int.SIZE_BYTES) { index ->
            bytes[8 + index] = (checksum ushr (index * Byte.SIZE_BITS)).toByte()
        }
    }

    private fun crypt(bytes: ByteArray, input: ByteArray, length: Int): ByteArray {
        val material = hkdf(input)
        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(material.copyOfRange(0, 16), "AES"),
            IvParameterSpec(material.copyOfRange(16, 28) + ByteArray(4)),
        )
        return bytes.copyOf().also {
            cipher.doFinal(bytes, 0, length).copyInto(it)
        }
    }

    private fun hkdf(input: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        val pseudoRandomKey = hmac(salt, input)
        var block = ByteArray(0)
        var counter = 1
        while (output.size() < 32) {
            block = hmac(pseudoRandomKey, block + info + counter.toByte())
            output.write(block)
            counter++
        }
        return output.toByteArray().copyOf(32)
    }

    private fun hmac(key: ByteArray, input: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(input)
    }

    private fun inflate(bytes: ByteArray) =
        InflaterInputStream(ByteArrayInputStream(bytes)).use { it.readBytes() }

    private fun deflate(bytes: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        DeflaterOutputStream(output).use { it.write(bytes) }
        return output.toByteArray()
    }

    private fun ByteArray.startsWith(prefix: ByteArray) =
        size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }
}

internal val loadProtectedDexPatch = rawResourcePatch {
    dependsOn(bypassProtectionIntegrityPatch)

    execute {
        ProtectedDex.load(get("assets"))
    }

    finalize {
        ProtectedDex.write()
    }
}