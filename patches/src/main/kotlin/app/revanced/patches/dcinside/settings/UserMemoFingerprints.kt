package app.revanced.patches.dcinside.settings

import app.morphe.patcher.Fingerprint

private const val USER_MEMO_EXTENSION_CLASS =
    "Lapp/revanced/extension/dcinside/settings/UserMemoPatch;"

internal object UserMemoRegisterFingerprint : Fingerprint(
    returnType = "Z",
    strings = listOf(
        "galleryId",
        "keyValues",
        "userValue",
        "memo",
    ),
    custom = { method, _ ->
        method.parameterTypes.size == 5 &&
            method.parameterTypes[1].toString() == "Ljava/lang/String;" &&
            method.parameterTypes[2].toString() == "Ljava/lang/String;" &&
            method.parameterTypes[3].toString() == "Z" &&
            method.parameterTypes[4].toString().startsWith("[")
    },
)

internal object UserMemoOpenRealmFingerprint : Fingerprint(
    definingClass = USER_MEMO_EXTENSION_CLASS,
    name = "openDefaultRealm",
)

internal object UserMemoNewPairArrayFingerprint : Fingerprint(
    definingClass = USER_MEMO_EXTENSION_CLASS,
    name = "newMemoPairArray",
)

internal object UserMemoNewPairFingerprint : Fingerprint(
    definingClass = USER_MEMO_EXTENSION_CLASS,
    name = "newMemoPair",
)

internal object UserMemoRegisterEntriesFingerprint : Fingerprint(
    definingClass = USER_MEMO_EXTENSION_CLASS,
    name = "registerEntriesWithApp",
)

internal object UserMemoBeginTransactionFingerprint : Fingerprint(
    definingClass = USER_MEMO_EXTENSION_CLASS,
    name = "beginUserMemoTransaction",
)

internal object UserMemoQueryEntriesFingerprint : Fingerprint(
    definingClass = USER_MEMO_EXTENSION_CLASS,
    name = "queryAllUserMemoEntries",
)

internal object UserMemoQueryContainersFingerprint : Fingerprint(
    definingClass = USER_MEMO_EXTENSION_CLASS,
    name = "queryAllUserMemoContainers",
)

internal object UserMemoQueryCountsFingerprint : Fingerprint(
    definingClass = USER_MEMO_EXTENSION_CLASS,
    name = "queryAllUserMemoCounts",
)

internal object UserMemoCommitTransactionFingerprint : Fingerprint(
    definingClass = USER_MEMO_EXTENSION_CLASS,
    name = "commitUserMemoTransaction",
)

internal object UserMemoTransactionActiveFingerprint : Fingerprint(
    definingClass = USER_MEMO_EXTENSION_CLASS,
    name = "isUserMemoTransactionActive",
)

internal object UserMemoCancelTransactionFingerprint : Fingerprint(
    definingClass = USER_MEMO_EXTENSION_CLASS,
    name = "cancelUserMemoTransaction",
)

internal object UserMemoReloadCacheFingerprint : Fingerprint(
    definingClass = USER_MEMO_EXTENSION_CLASS,
    name = "reloadUserMemoCache",
)

internal object UserMemoDeleteRealmObjectFingerprint : Fingerprint(
    definingClass = USER_MEMO_EXTENSION_CLASS,
    name = "deleteRealmObject",
)
