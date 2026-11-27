package app.revanced.patches.dcinside.layout.hide.pageindicator

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.resource.ResourceType
import app.morphe.patcher.resourceLiteral
import app.revanced.patches.dcinside.interaction.gallerywatch.PostListAdapterConstructorFingerprint
import app.revanced.patches.dcinside.shared.recyclerViewHolderType

internal object PostListPageIndicatorFingerprint : Fingerprint(
    classFingerprint = PostListAdapterConstructorFingerprint,
    name = "onBindViewHolder",
    parameters = listOf(recyclerViewHolderType, "I"),
    returnType = "V",
    filters = listOf(resourceLiteral(ResourceType.STRING, "post_section_text")),
)

internal object SearchResultPageIndicatorFingerprint : Fingerprint(
    name = "onBindViewHolder",
    parameters = listOf(recyclerViewHolderType, "I"),
    returnType = "V",
    filters = listOf(resourceLiteral(ResourceType.STRING, "post_section_text")),
    custom = { _, classDef -> classDef.type != PostListAdapterConstructorFingerprint.originalClassDef.type },
)