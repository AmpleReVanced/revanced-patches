package app.revanced.patches.kakaotalk.tab.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object NowFragmentOnViewCreatedFingerprint : Fingerprint(
    parameters = listOf("Landroid/view/View;", "Landroid/os/Bundle;"),
    returnType = "V",
    custom = { method, classDef ->
        classDef.sourceFile == "NowFragment.kt" && method.name == "onViewCreated"
    }
)

internal object NowTabPagerAdapterFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = listOf("I"),
    returnType = "Landroidx/fragment/app/Fragment;",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.SGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.IF_NE,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_INTERFACE,
    ),
    custom = { method, classDef ->
        classDef.sourceFile == "NowTabPagerAdapter.kt"
    }
)

internal object GetOpenLinkModuleFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    parameters = listOf(),
    returnType = "Lcom/kakao/talk/module/openlink/contract/OpenLinkModuleFacade;",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.SGET_OBJECT,
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CHECK_CAST,
        Opcode.RETURN_OBJECT,
    ),
    custom = { method, classDef -> classDef.sourceFile == "ModuleFacades.kt" }
)

internal object TransitionOpenLinkOrShortformMethodFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = listOf(),
    returnType = "V",
    strings = listOf("t", "n"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.IGET_OBJECT,
        Opcode.IF_EQZ,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.SGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.SGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CHECK_CAST,
        Opcode.SGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.AGET,
    ),
    custom = { method, classDef -> classDef.sourceFile == "NowFragment.kt" }
)

internal object ChooseOpenLinkTabFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "V",
    custom = { method, classDef ->
        val instructions = method.implementation?.instructions
        classDef.sourceFile == "NowFragment.kt" &&
                method.parameterTypes.size == 2 &&
                method.parameterTypes.last() == "Landroid/view/View;" &&
                instructions?.any {
                    it.opcode == Opcode.SGET_OBJECT &&
                            it.getReference<FieldReference>()?.name == "Openlink"
                } == true &&
                instructions.any {
                    val reference = it.getReference<MethodReference>()
                    it.opcode == Opcode.INVOKE_VIRTUAL &&
                            reference?.definingClass == "Landroidx/viewpager2/widget/ViewPager2;" &&
                            reference.parameterTypes == listOf("I", "Z") &&
                            reference.returnType == "V"
                }
    }
)

internal object ChooseNowChildTabFingerprint : Fingerprint(
    name = "invokeSuspend",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/Object;",
    strings = listOf(
        "call to 'resume' before 'invoke' with coroutine",
        "NOW_TAB",
        "binding",
        "viewpager",
        "hsv"
    ),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IGET,
        Opcode.CONST_4,
        Opcode.CONST_4,
        Opcode.CONST_4,
        Opcode.IF_EQZ,
        Opcode.IF_EQ,
        Opcode.IF_NE,
        Opcode.INVOKE_STATIC,
        Opcode.GOTO_16,
        Opcode.NEW_INSTANCE,
        Opcode.CONST_STRING,
        Opcode.INVOKE_DIRECT,
        Opcode.THROW,
        Opcode.IGET_OBJECT,
        Opcode.CHECK_CAST,
        Opcode.IGET_OBJECT,
        Opcode.CHECK_CAST,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_OBJECT
    ),
    custom = { method, classDef -> classDef.sourceFile == "NowFragment.kt" }
)