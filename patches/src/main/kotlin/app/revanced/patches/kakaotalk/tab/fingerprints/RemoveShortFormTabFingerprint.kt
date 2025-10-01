package app.revanced.patches.kakaotalk.tab.fingerprints

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val nowFragmentOnViewCreatedFingerprint = fingerprint {
    parameters("Landroid/view/View;", "Landroid/os/Bundle;")
    returns("V")
    custom { method, classDef ->
        classDef.sourceFile == "NowFragment.kt" && method.name == "onViewCreated"
    }
}

internal val nowTabPagerAdapterFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC)
    parameters("I")
    returns("Landroidx/fragment/app/Fragment;")
    opcodes(
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CONST_STRING,
        Opcode.INVOKE_STATIC,
        Opcode.CHECK_CAST,
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_INTERFACE,
    )
    custom { method, classDef ->
        classDef.sourceFile == "NowTabPagerAdapter.kt"
    }
}

internal val getOpenLinkModuleFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL)
    parameters()
    returns("Lcom/kakao/talk/module/openlink/contract/OpenLinkModuleFacade;")
    opcodes(
        Opcode.SGET_OBJECT,
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CHECK_CAST,
        Opcode.RETURN_OBJECT,
    )
    custom { method, classDef -> classDef.sourceFile == "ModuleFacades.kt" }
}

internal val transitionOpenLinkOrShortformMethodFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC)
    parameters()
    returns("V")
    strings("t", "n")
    opcodes(
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
    )
    custom { method, classDef -> classDef.sourceFile == "NowFragment.kt" }
}

internal val chooseTabShortFormOrOpenLinkFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameters("Landroid/os/Bundle;")
    returns("V")
    strings("NOW_TAB", "binding", "current_tab")
    opcodes(
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT,
        Opcode.CONST_4,
        Opcode.IF_EQZ,
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.MOVE_OBJECT,
        Opcode.CHECK_CAST,
    )
    custom { method, classDef -> classDef.sourceFile == "NowFragment.kt" }
}

internal val fragmentMainTabNowBindingFingerprint = fingerprint {
    custom { _, classDef -> classDef.sourceFile == "FragmentMainTabNowBinding.java" }
}

internal val viewPager2VerifyFingerprint = fingerprint {
    strings("Cannot change current item when ViewPager2 is fake dragging")
    custom { _, classDef -> classDef.sourceFile == "ViewPager2.java" }
}