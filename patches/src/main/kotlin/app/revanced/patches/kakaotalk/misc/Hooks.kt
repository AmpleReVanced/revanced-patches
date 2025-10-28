package app.revanced.patches.kakaotalk.misc

import app.revanced.patches.shared.misc.extension.extensionHook

internal val initHook = extensionHook {
    custom { method, classDef ->
        method.name == "onCreate" && classDef.type == "Lcom/kakao/talk/application/App;"
    }
}
