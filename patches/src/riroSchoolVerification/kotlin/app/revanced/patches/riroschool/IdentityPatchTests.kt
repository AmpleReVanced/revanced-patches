package app.revanced.patches.riroschool

import app.revanced.patches.riroschool.misc.analytics.*
import app.revanced.patches.riroschool.misc.integrity.*
import app.revanced.patches.riroschool.shared.*

import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import java.io.File
import app.revanced.extension.riroschool.identity.FormParameters
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OffsetInstruction
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/** Check native UUID omission and preservation of unrelated form fields against the original APK. */
fun runIdentityPatchTests() {
    runFinalFormFilterTests()
    val apk = File(System.getenv("RIRO_TEST_APK") ?: error("Set RIRO_TEST_APK to the original Riro School 7.5.6 APK"))
    val dex = DexFileFactory.loadDexFile(apk, Opcodes.getDefault())
    val helper = dex.classes.single { it.type == "Lg6/i;" }
    for (name in listOf("g", "h")) {
        val original = helper.methods.single { it.name == name }
        val renamed = MutableMethod(renameDeviceFixture(original))
        omitNativeDeviceId(renamed)
        check("&device_id=" !in identityReferences(renamed))
        check(identityReferences(renamed).count { "FormParameters;->withoutDeviceId(" in it } == 1)
        val before = identityReferences(MutableMethod(original))
        check("&device_id=" in before)
        val copy = MutableMethod(original)
        omitNativeDeviceId(copy)
        val after = identityReferences(copy)
        check("&device_id=" !in after)
        check(after.none { "Lg6/i;->j(" in it })
        check(after.count { "FormParameters;->withoutDeviceId(" in it } == 1) {
            "Identity: final form filter missing; extra can re-inject device_id or %64evice_id"
        }
        assertFinalFormFilterBoundary(copy)
        for (field in listOf("fcmCode=", "&app_version=", "&site_id=", "&site_name=", "&cookie_token=")) {
            check(after.count { it == field } == before.count { it == field }) { "unrelated field changed: $field" }
        }
        check(after.count { "URLEncoder;->encode" in it } == before.count { "URLEncoder;->encode" in it } - 1)
        check(runCatching { omitNativeDeviceId(copy) }.isFailure)
    }
    check(identityReferences(MutableMethod(helper.methods.single { it.name == "j" })).contains("SAVE_KEY_UUID"))
}

/** Exercise exact root-key filtering, raw-byte/order preservation, duplicates and malformed encodings. */
private fun runFinalFormFilterTests() {
    val cases = listOf(
        "device_id=x&a=1" to "a=1",
        "a=1&device_id=x&b=2" to "a=1&b=2",
        "a=1&device_id=x" to "a=1",
        "device_id=x" to "",
        "device_id" to "",
        "a=1&device_id&b=2" to "a=1&b=2",
        "device_id=x&device_id=y&keep=1&%64evice_id=z&keep=2" to "keep=1&keep=2",
        "%64evice%5fid=x&a=1" to "a=1",
        "%64%65%76%69%63%65%5F%69%64=x" to "",
        "a=1&&device_id=x&&b=2&" to "a=1&&&b=2&",
        "&device_id=x&" to "&",
        "=device_id&deviceid=BLE&uid=u&uuid=q&macaddress=m&device_id=x" to "=device_id&deviceid=BLE&uid=u&uuid=q&macaddress=m",
        "cookie_token=device_id%3Dx%26z%3D1&token=device_id=x&device_id=drop" to "cookie_token=device_id%3Dx%26z%3D1&token=device_id=x",
        "a=%2f+%2F&한글=값&dup=1&device_id=x&dup=2&empty=" to "a=%2f+%2F&한글=값&dup=1&dup=2&empty=",
        "device+id=x&device%20id=y&Device_id=z&%2564evice_id=q" to "device+id=x&device%20id=y&Device_id=z&%2564evice_id=q",
        "%64evice_id%=x&device_id%ZZ=y&%FFdevice_id=z&%C0%AFdevice_id=q" to "%64evice_id%=x&device_id%ZZ=y&%FFdevice_id=z&%C0%AFdevice_id=q",
        "%６４evice_id=x&%=y&%2=z&%G0=q" to "%６４evice_id=x&%=y&%2=z&%G0=q",
        "a=1&%E3%81=x&device_id=drop&cookie_token=%ZZ" to "a=1&%E3%81=x&cookie_token=%ZZ",
        "device_id=x=y&%E9%9F%93=한글&cookie_token=a=b=c" to "%E9%9F%93=한글&cookie_token=a=b=c",
        "" to "",
        "&&" to "&&",
    )
    check(FormParameters.withoutDeviceId(null) == null)
    for ((body, expected) in cases) {
        val actual = FormParameters.withoutDeviceId(body)
        check(actual == expected) { "Identity form mismatch: expected=$expected actual=$actual" }
        check(actual.toByteArray(Charsets.UTF_8).contentEquals(expected.toByteArray(Charsets.UTF_8)))
        check(FormParameters.withoutDeviceId(actual) == actual) { "Identity filter must be idempotent" }
    }
    verifyLargeNakedForm()
}

/** Ensure a six-megabyte form with one million naked fields is processed within five seconds without rescanning the remaining input for each key. */
private fun verifyLargeNakedForm() {
    val prefix = "naked&".repeat(1_000_000)
    val input = prefix + "device_id=drop"
    val completed = CompletableFuture<String>()
    Thread {
        try { completed.complete(FormParameters.withoutDeviceId(input)) }
        catch (error: Throwable) { completed.completeExceptionally(error) }
    }.apply { isDaemon = true; name = "large-naked-form-regression"; start() }
    val actual = completed.get(5, TimeUnit.SECONDS)
    check(actual == prefix.dropLast(1)) { "Large naked form fields or separators changed" }
}

/** Check that the final filter result is used by the original getBytes receiver with the original encoding contract. */
private fun assertFinalFormFilterBoundary(method: MutableMethod) {
    val instructions = method.implementation!!.instructions.toList()
    val index = instructions.indices.single { index ->
        ((instructions[index] as? ReferenceInstruction)?.reference as? MethodReference)?.let {
            it.definingClass == "Ljava/lang/String;" && it.name == "getBytes" && it.parameterTypes.isEmpty()
        } == true
    }
    val receiver = (instructions[index] as FiveRegisterInstruction).registerC
    val filter = instructions[index - 2] as ReferenceInstruction
    check((filter.reference as MethodReference).definingClass == "Lapp/revanced/extension/riroschool/identity/FormParameters;")
    check((filter as RegisterRangeInstruction).startRegister == receiver)
    check((instructions[index - 1] as OneRegisterInstruction).registerA == receiver)
    val getBytesAddress = instructions.take(index).sumOf { it.codeUnits }
    var address = 0
    for (instruction in instructions) {
        check(instruction !is OffsetInstruction || address + instruction.codeOffset != getBytesAddress) {
            "Identity: an original branch bypasses the final form filter"
        }
        address += instruction.codeUnits
    }
}

/** Normalize identity-related strings and method references for comparisons. */
private fun identityReferences(method: MutableMethod): List<String> = method.implementation!!.instructions.mapNotNull {
    when (val ref = (it as? ReferenceInstruction)?.reference) {
        is StringReference -> ref.string
        is MethodReference -> "${ref.definingClass}->${ref.name}(${ref.parameterTypes.joinToString("")})${ref.returnType}"
        else -> null
    }
}
