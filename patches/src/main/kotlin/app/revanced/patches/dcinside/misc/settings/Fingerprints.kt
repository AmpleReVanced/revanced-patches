package app.revanced.patches.dcinside.misc.settings

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resourceLiteral

private object SettingsFragmentFingerprint : Fingerprint(
    name = "onCreateView",
    parameters = listOf("Landroid/view/LayoutInflater;", "Landroid/view/ViewGroup;", "Landroid/os/Bundle;"),
    returnType = "Landroid/view/View;",
    filters = listOf(resourceLiteral(ResourceType.LAYOUT, "fragment_settings")),
)

internal object SettingsFragmentOnViewCreatedFingerprint : Fingerprint(
    classFingerprint = SettingsFragmentFingerprint,
    name = "onViewCreated",
    parameters = listOf("Landroid/view/View;", "Landroid/os/Bundle;"),
    returnType = "V",
)
