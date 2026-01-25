package app.revanced.patches.kakaotalk.tab.fingerprints

import app.revanced.patcher.fingerprint

// Sorry to proguard :(
internal val isChatListCollapseButtonEnabledFingerprint = fingerprint {
    custom { method, classDef -> classDef.sourceFile == "OpenChatTabFeedContract.kt" && method.name == "d" && classDef.fields.toList().size == 5 }
}