package app.revanced.patches.kakaotalk.tab.fingerprints

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fingerprint

// Sorry to proguard :(
internal object IsChatListCollapseButtonEnabledFingerprint : Fingerprint(
    custom = { method, classDef -> classDef.sourceFile == "OpenChatTabFeedContract.kt"
            && method.name == "m" && classDef.fields.toList().size == 9 }
)