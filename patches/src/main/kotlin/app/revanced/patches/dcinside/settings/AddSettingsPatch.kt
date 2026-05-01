package app.revanced.patches.dcinside.settings

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.all.misc.resources.addResourcesPatch
import app.morphe.util.getReference
import app.revanced.patches.dcinside.misc.addExtensionPatch
import app.revanced.patches.dcinside.misc.sharedExtensionPatch
import app.revanced.patches.dcinside.shared.Constants.COMPATIBILITY_DC_INSIDE
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val addSettingsPatch = bytecodePatch(
    name = "Add settings",
    description = "Adds a Morphe settings entry to the DCInside settings screen.",
) {
    compatibleWith(COMPATIBILITY_DC_INSIDE)
    dependsOn(
        addExtensionPatch,
        addResourcesPatch,
        addSettingsResourcesPatch,
        registerSettingsActivityPatch,
        sharedExtensionPatch,
    )

    execute {
        SettingsFragmentOnViewCreatedFingerprint.method.addInstructionsAfterOnViewCreatedSuperCall(
            superClass = "Landroidx/fragment/app/Fragment;",
            ownerName = "SettingsFragment",
            smaliInstructions = """
                invoke-static {p1}, Lapp/revanced/extension/dcinside/settings/SettingsActivity;->bindSettingsShortcut(Landroid/view/View;)V
            """.trimIndent(),
        )

        val userMemoRegisterMethod = UserMemoRegisterFingerprint.method
        val realmType = userMemoRegisterMethod.parameterTypes[0].toString()
        val pairArrayType = userMemoRegisterMethod.parameterTypes[4].toString()
        val pairType = pairArrayType.removePrefix("[")
        val defaultRealmMethod = run {
            var methodName: String? = null
            classDefForEach { classDef ->
                if (classDef.type != realmType) return@classDefForEach

                methodName = classDef.methods.singleOrNull { method ->
                    method.parameterTypes.isEmpty() &&
                        method.returnType == realmType &&
                        method.accessFlags and AccessFlags.STATIC.value != 0
                }?.name
            }
            methodName ?: throw PatchException("Could not find default Realm opener for $realmType")
        }

        UserMemoPresetOpenRealmFingerprint.method.addInstructions(
            0,
            """
                invoke-static {}, $realmType->$defaultRealmMethod()$realmType
                move-result-object p0
                return-object p0
            """.trimIndent(),
        )

        UserMemoPresetNewPairArrayFingerprint.method.addInstructions(
            0,
            """
                new-array p1, p0, $pairArrayType
                return-object p1
            """.trimIndent(),
        )

        UserMemoPresetNewPairFingerprint.method.addInstructions(
            0,
            """
                new-instance p2, $pairType
                invoke-direct {p2, p0, p1}, $pairType-><init>(Ljava/lang/Object;Ljava/lang/Object;)V
                return-object p2
            """.trimIndent(),
        )

        UserMemoPresetRegisterEntriesFingerprint.method.addInstructions(
            0,
            """
                check-cast p0, $realmType
                check-cast p1, $pairArrayType
                invoke-static {p0, p2, p3, p4, p1}, ${userMemoRegisterMethod.definingClass}->${userMemoRegisterMethod.name}(${realmType}Ljava/lang/String;Ljava/lang/String;Z${pairArrayType})Z
                move-result p0
                return p0
            """.trimIndent(),
        )
    }
}

private fun MutableMethod.addInstructionsAfterOnViewCreatedSuperCall(
    superClass: String,
    ownerName: String,
    smaliInstructions: String,
) {
    val onViewCreatedSuperCallIndex = instructions.indexOfFirst {
        if (it.opcode != Opcode.INVOKE_SUPER) return@indexOfFirst false

        val reference = it.getReference<MethodReference>() ?: return@indexOfFirst false
        reference.definingClass == superClass &&
            reference.name == "onViewCreated" &&
            reference.returnType == "V"
    }

    check(onViewCreatedSuperCallIndex >= 0) {
        "Could not find $ownerName.onViewCreated super call in $definingClass->$name"
    }

    addInstructions(onViewCreatedSuperCallIndex + 1, smaliInstructions)
}
