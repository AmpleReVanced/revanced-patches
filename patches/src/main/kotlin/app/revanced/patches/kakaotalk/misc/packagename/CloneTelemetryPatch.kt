package app.revanced.patches.kakaotalk.misc.packagename

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.returnEarly
import app.morphe.util.asSequence
import app.morphe.util.getNode
import org.w3c.dom.Element

private val cloneTelemetryResourcePatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { document ->
            val application = document.getNode("application") as Element
            mapOf(
                "firebase_analytics_collection_deactivated" to "true",
                "firebase_sessions_enabled" to "false",
                "firebase_crashlytics_collection_enabled" to "false",
                "delivery_metrics_exported_to_big_query_enabled" to "false",
            ).forEach { (name, value) ->
                val metadata = application.childNodes.asSequence().filterIsInstance<Element>().find {
                    it.tagName == "meta-data" && it.getAttribute("android:name") == name
                } ?: application.ownerDocument.createElement("meta-data").also {
                    it.setAttribute("android:name", name)
                    application.appendChild(it)
                }
                metadata.setAttribute("android:value", value)
            }
            application.childNodes.asSequence().filterIsInstance<Element>().filter {
                it.getAttribute("android:name").startsWith("com.google.android.gms.measurement.AppMeasurement")
            }.forEach { it.setAttribute("android:enabled", "false") }
        }
    }
}

internal val cloneTelemetryPatch = bytecodePatch {
    dependsOn(cloneTelemetryResourcePatch)

    execute {
        listOf(
            AdFitMatrixInitializerFingerprint,
            OnePassLoggerInitializerFingerprint,
            AnalyticsServiceConnectionFingerprint,
            FirebaseSessionSendFingerprint,
            FirebaseDeliveryMetricsSendFingerprint,
        ).forEach { it.method.returnEarly() }

        MlKitTransportConstructorFingerprint.matchAll(5..5).forEach {
            mlKitTransportSendFingerprint().match(it.classDef).method.returnEarly()
        }

        AnalyticsCollectionFingerprint.method.returnEarly(1)
        AnalyticsRegistrarFingerprint.method.addInstructions(
            0,
            """
                invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;
                move-result-object v0
                return-object v0
            """.trimIndent(),
        )

        CrashlyticsReportSendFingerprint.method.addInstructions(
            0,
            """
                invoke-static {p1}, ${TaskForResultFingerprint.method}
                move-result-object p1
                return-object p1
            """.trimIndent(),
        )
    }
}