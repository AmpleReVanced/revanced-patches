package app.revanced.patches.riroschool

import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.riroschool.misc.analytics.omitNativeDeviceId
import app.revanced.patches.riroschool.misc.analytics.omitNativeFcmCode
import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.immutable.*
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore
import com.android.tools.smali.dexlib2.writer.pool.DexPool
import java.io.File

/** Proves that Firebase and identity transformations commute on both original form builders. */
internal fun runNativeCombinationTests() {
    val dex = DexFileFactory.loadDexContainer(File(requireNotNull(System.getenv("RIRO_TEST_APK"))), Opcodes.getDefault())
    val owner = dex.dexEntryNames.flatMap { dex.getEntry(it)!!.dexFile.classes }.single { it.type == "Lg6/i;" }
    for (method in owner.methods.filter { it.name in setOf("g", "h") }) {
        val firebaseFirst = MutableMethod(method).also { omitNativeFcmCode(it); omitNativeDeviceId(it) }
        val identityFirst = MutableMethod(method).also { omitNativeDeviceId(it); omitNativeFcmCode(it) }
        check(combinationDex(owner, firebaseFirst).contentEquals(combinationDex(owner, identityFirst))) {
            "Native form transformations depend on patch order: ${method.name}"
        }
    }
}

/** Serializes executable method structure without debug-address remapping noise. */
private fun combinationDex(owner: ClassDef, method: Method): ByteArray {
    val implementation = requireNotNull(method.implementation)
    val stripped = ImmutableMethod(method.definingClass, method.name, method.parameters, method.returnType,
        method.accessFlags, method.annotations, method.hiddenApiRestrictions,
        ImmutableMethodImplementation(implementation.registerCount, implementation.instructions, implementation.tryBlocks, emptyList()))
    val definition = ImmutableClassDef(owner.type, owner.accessFlags, owner.superclass, owner.interfaces,
        owner.sourceFile, owner.annotations, owner.fields, listOf(stripped))
    val store = MemoryDataStore()
    return try { DexPool.writeTo(store, ImmutableDexFile(Opcodes.getDefault(), listOf(definition))); store.data } finally { store.close() }
}