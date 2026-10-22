package app.revanced.patches.kakaotalk.misc.integrity

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.cloneParameters
import app.morphe.patcher.util.findMutableMethodOf
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.kakaotalk.misc.integrity.fingerprints.MoatConnectionFactoryFingerprint
import app.revanced.patches.kakaotalk.misc.integrity.fingerprints.MoatForceOffReportFingerprint
import app.revanced.patches.kakaotalk.misc.integrity.fingerprints.MoatForceOffRepositoryFingerprint
import app.revanced.patches.kakaotalk.misc.integrity.fingerprints.MoatMonitorFingerprint
import app.revanced.patches.kakaotalk.misc.integrity.fingerprints.MoatNetworkFingerprint
import app.revanced.patches.kakaotalk.misc.integrity.fingerprints.MoatSecurityInfoFingerprint
import app.revanced.patches.kakaotalk.misc.integrity.fingerprints.moatTiaraTrackerFingerprint
import app.revanced.util.parameterTypeNames
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

context(context: BytecodePatchContext)
private fun MutableMethod.bypassMoat(
    registerCount: Int = 1,
    body: (List<Int>) -> String,
) {
    val method = cloneParameters()
    val free = method.getFreeRegisterProvider(0, registerCount)
    val registers = List(registerCount) { free.getFreeRegister4Bit() }
    method.addInstructionsWithLabels(
        0,
        """
            invoke-static {}, $BYPASS_MOAT
            move-result v${registers.first()}
            if-eqz v${registers.first()}, :morphe_moat_original
            ${body(registers)}
            :morphe_moat_original
            nop
        """.trimIndent(),
    )
}

internal fun BytecodePatchContext.blockMoatTransmission() {
    MoatSecurityInfoFingerprint.method.bypassMoat(3) { (result, value, success) ->
        """
            invoke-static/range {p1 .. p1}, $EXTENSION_CLASS->getSecurityInfo(Ljava/lang/Enum;)Ljava/lang/String;
            move-result-object v$value
            new-instance v$result, Lkotlin/Pair;
            sget-object v$success, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;
            invoke-direct {v$result, v$success, v$value}, Lkotlin/Pair;-><init>(Ljava/lang/Object;Ljava/lang/Object;)V
            return-object v$result
        """
    }

    val network = MoatNetworkFingerprint.classDef
    val connectionFactory = network.methods.flatMap { method ->
        method.instructions.mapNotNull { it.getReference<MethodReference>() }.toList()
    }.filter {
        it.returnType == "Ljavax/net/ssl/HttpsURLConnection;" &&
                it.parameterTypes.firstOrNull() == "Ljava/net/URL;"
    }.distinct().singleOrNull()
        ?: throw PatchException("Could not resolve the Moat HTTPS connection factory.")
    MoatConnectionFactoryFingerprint.match(classDefBy(connectionFactory.definingClass))
        .method.bypassMoat { (result) ->
            """
                new-instance v$result, Ljava/io/IOException;
                invoke-direct {v$result}, Ljava/io/IOException;-><init>()V
                throw v$result
            """
        }

    val nativeRequests = network.methods.filter {
        AccessFlags.STATIC.isSet(it.accessFlags) &&
                it.parameterTypeNames in listOf(
                    listOf("Ljava/lang/String;", "Ljava/util/Map;", "I"),
                    listOf("Ljava/lang/String;", "Ljava/util/Map;", "[B", "I"),
                ) && it.returnType in listOf("I", "[B")
    }
    if (nativeRequests.size != 4) throw PatchException("Expected four Moat native HTTP request bridges.")
    nativeRequests.forEach { method ->
        mutableClassDefBy(method.definingClass).findMutableMethodOf(method).bypassMoat { (result) ->
            if (method.returnType == "I") {
                "const/4 v$result, -0x1\nreturn v$result"
            } else {
                "const/4 v$result, 0x0\nnew-array v$result, v$result, [B\nreturn-object v$result"
            }
        }
    }

    val monitors = MoatMonitorFingerprint.classDef.methods.filter {
        it.returnType == "V" && it.parameterTypeNames in listOf(
            listOf("J", "Ljava/lang/String;", "I"),
            listOf("Ljava/lang/String;", "J"),
        )
    }
    if (monitors.size != 2) throw PatchException("Expected two Moat monitoring callbacks.")
    monitors.forEach {
        mutableClassDefBy(it.definingClass).findMutableMethodOf(it).bypassMoat { "return-void" }
    }

    MoatForceOffReportFingerprint.method.apply {
        val unit = instructions.mapNotNull { it.getReference<FieldReference>() }.first {
            it.type == "Lkotlin/Unit;"
        }
        bypassMoat { (result) -> "sget-object v$result, $unit\nreturn-object v$result" }
    }
    MoatForceOffRepositoryFingerprint.method.apply {
        val resultConstructor = instructions.mapNotNull { it.getReference<MethodReference>() }.last {
            it.name == "<init>" && it.parameterTypes == listOf("Ljava/lang/String;")
        }
        bypassMoat(2) { (result, value) ->
            """
                new-instance v$result, ${resultConstructor.definingClass}
                const-string v$value, ""
                invoke-direct {v$result, v$value}, $resultConstructor
                return-object v$result
            """
        }
    }

    val trackers = mutableListOf<String>()
    classDefForEach {
        if (it.sourceFile in setOf("PayMoatTiaraTracker.kt", "PaySecurityTiaraTrackerImpl.kt")) {
            trackers.add(it.type)
        }
    }
    if (trackers.size != 3) throw PatchException("Expected three Moat Tiara trackers.")
    trackers.forEach {
        moatTiaraTrackerFingerprint(it).method.bypassMoat { "return-void" }
    }
}