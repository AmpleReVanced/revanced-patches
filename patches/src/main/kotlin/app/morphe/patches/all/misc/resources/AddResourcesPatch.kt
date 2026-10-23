/*
 * Copyright 2025 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * File-Specific License Notice (GPLv3 Section 7 Terms)
 *
 * This file is part of the Morphe project and is licensed under
 * the GNU General Public License version 3 (GPLv3), with the Additional
 * Terms under Section 7 described in the LICENSE file.
 *
 * https://www.gnu.org/licenses/gpl-3.0.html
 *
 * Section 7b: Notice Preservation
 * -------------------------------
 * This entire comment block must be preserved in all copies,
 * distributions, and derivative works of this file, in both
 * original and modified source forms.
 *
 * Portions of this software are provided "AS IS" by the Morphe software project.
 * Any express or implied warranties, including the implied warranties of
 * merchantability and fitness for a particular purpose, are disclaimed.
 */

package app.morphe.patches.all.misc.resources

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.util.resource.StringResourceSanitizer.sanitizeAndroidResourceString
import app.morphe.patches.util.resource.forEachRawResourceElement
import app.morphe.util.inputStreamFromBundledResource
import java.io.File
import java.util.Locale
import java.util.logging.Logger
import kotlin.collections.listOf

internal val localesYouTube = listOf(
    AppLocale("", ""), // Default English locale. Must be first.
    AppLocale("af-rZA", "af"),
    AppLocale("am-rET", "am"),
    AppLocale("ar-rSA", "ar"),
    AppLocale("as-rIN", "as"),
    AppLocale("az-rAZ", "az"),
    AppLocale("be-rBY", "be"),
    AppLocale("bg-rBG", "bg"),
    AppLocale("bn-rBD", "bn"),
    AppLocale("bs-rBA", "bs"),
    AppLocale("ca-rES", "ca"),
    AppLocale("cs-rCZ", "cs"),
    AppLocale("da-rDK", "da"),
    AppLocale("de-rDE", "de"),
    AppLocale("el-rGR", "el"),
    AppLocale("es-rES", "es"),
    AppLocale("et-rEE", "et"),
    AppLocale("eu-rES", "eu"),
    AppLocale("fa-rIR", "fa"),
    AppLocale("fi-rFI", "fi"),
    AppLocale("fil-rPH", "tl"),
    AppLocale("fr-rFR", "fr"),
    AppLocale("gl-rES", "gl"),
    AppLocale("gu-rIN", "gu"),
    AppLocale("hi-rIN", "hi"),
    AppLocale("hr-rHR", "hr"),
    AppLocale("hu-rHU", "hu"),
    AppLocale("hy-rAM", "hy"),
    AppLocale("in-rID", "in"),
    AppLocale("is-rIS", "is"),
    AppLocale("it-rIT", "it"),
    AppLocale("iw-rIL", "iw"),
    AppLocale("ja-rJP", "ja"),
    AppLocale("ka-rGE", "ka"),
    AppLocale("kk-rKZ", "kk"),
    AppLocale("km-rKH", "km"),
    AppLocale("kn-rIN", "kn"),
    AppLocale("ko-rKR", "ko"),
    AppLocale("ky-rKG", "ky"),
    AppLocale("lo-rLA", "lo"),
    AppLocale("lt-rLT", "lt"),
    AppLocale("lv-rLV", "lv"),
    AppLocale("mk-rMK", "mk"),
    AppLocale("ml-rIN", "ml"),
    AppLocale("mn-rMN", "mn"),
    AppLocale("mr-rIN", "mr"),
    AppLocale("ms-rMY", "ms"),
    AppLocale("my-rMM", "my"),
    AppLocale("nb-rNO", "nb"),
    AppLocale("ne-rNP", "ne"),
    AppLocale("nl-rNL", "nl"),
    AppLocale("or-rIN", "or"),
    AppLocale("pa-rIN", "pa"),
    AppLocale("pl-rPL", "pl"),
    AppLocale("pt-rBR", "pt-rBR"),
    AppLocale("pt-rPT", "pt-rPT"),
    AppLocale("ro-rRO", "ro"),
    AppLocale("ru-rRU", "ru"),
    AppLocale("si-rLK", "si"),
    AppLocale("sk-rSK", "sk"),
    AppLocale("sl-rSI", "sl"),
    AppLocale("sq-rAL", "sq"),
    AppLocale("sr-rCS", "b+sr+Latn"),
    AppLocale("sr-rSP", "sr"),
    AppLocale("sv-rSE", "sv"),
    AppLocale("sw-rKE", "sw"),
    AppLocale("ta-rIN", "ta"),
    AppLocale("te-rIN", "te"),
    AppLocale("th-rTH", "th"),
    AppLocale("tr-rTR", "tr"),
    AppLocale("uk-rUA", "uk"),
    AppLocale("ur-rIN", "ur"),
    AppLocale("uz-rUZ", "uz"),
    AppLocale("vi-rVN", "vi"),
    AppLocale("zh-rCN", "zh-rCN"),
    AppLocale("zh-rTW", "zh-rTW"),
    AppLocale("zu-rZA", "zu"),
    // Languages not found in YouTube.
    AppLocale("ga-rIE", "ga", false),
    AppLocale("kmr-rTR", "kmr", false),
    AppLocale("ckb-rIR", "ckb", false)
)

internal val localesReddit = listOf(
    AppLocale("", ""), // Default English locale. Must be first.
    AppLocale("ar-rSA", "ar"),
    AppLocale("de-rDE", "de"),
    AppLocale("es-rES", "es"),
    AppLocale("fi-rFI", "fi"),
    AppLocale("fr-rFR", "fr"),
    AppLocale("hi-rIN", "hi"),
    AppLocale("hu-rHU", "hu"),
    AppLocale("it-rIT", "it"),
    AppLocale("ja-rJP", "ja"),
    AppLocale("ko-rKR", "ko"),
    AppLocale("ms-rMY", "ms"),
    AppLocale("nl-rNL", "nl"),
    AppLocale("pl-rPL", "pl"),
    AppLocale("pt-rBR", "pt-rBR"),
    AppLocale("pt-rPT", "pt-rPT"),
    AppLocale("ru-rRU", "ru"),
    AppLocale("th-rTH", "th"),
    AppLocale("tr-rTR", "tr"),
    AppLocale("uk-rUA", "uk"),
    AppLocale("vi-rVN", "vi"),
    AppLocale("zh-rCN", "zh-rCN"),
    AppLocale("zh-rTW", "zh-rTW"),

    // Languages not found in Reddit.
    AppLocale("af-rZA", "af", false),
    AppLocale("am-rET", "am", false),
    AppLocale("as-rIN", "as", false),
    AppLocale("az-rAZ", "az", false),
    AppLocale("be-rBY", "be", false),
    AppLocale("bg-rBG", "bg", false),
    AppLocale("bn-rBD", "bn", false),
    AppLocale("bs-rBA", "bs", false),
    AppLocale("ca-rES", "ca", false),
    AppLocale("cs-rCZ", "cs", false),
    AppLocale("da-rDK", "da", false),
    AppLocale("el-rGR", "el", false),
    AppLocale("et-rEE", "et", false),
    AppLocale("eu-rES", "eu", false),
    AppLocale("fa-rIR", "fa", false),
    AppLocale("fil-rPH", "tl", false),
    AppLocale("gl-rES", "gl", false),
    AppLocale("gu-rIN", "gu", false),
    AppLocale("hr-rHR", "hr", false),
    AppLocale("hy-rAM", "hy", false),
    AppLocale("in-rID", "in", false),
    AppLocale("is-rIS", "is", false),
    AppLocale("iw-rIL", "iw", false),
    AppLocale("ka-rGE", "ka", false),
    AppLocale("kk-rKZ", "kk", false),
    AppLocale("km-rKH", "km", false),
    AppLocale("kn-rIN", "kn", false),
    AppLocale("ky-rKG", "ky", false),
    AppLocale("lo-rLA", "lo", false),
    AppLocale("lt-rLT", "lt", false),
    AppLocale("lv-rLV", "lv", false),
    AppLocale("mk-rMK", "mk", false),
    AppLocale("ml-rIN", "ml", false),
    AppLocale("mn-rMN", "mn", false),
    AppLocale("mr-rIN", "mr", false),
    AppLocale("my-rMM", "my", false),
    AppLocale("nb-rNO", "nb", false),
    AppLocale("ne-rNP", "ne", false),
    AppLocale("or-rIN", "or", false),
    AppLocale("pa-rIN", "pa", false),
    AppLocale("ro-rRO", "ro", false),
    AppLocale("sk-rSK", "sk", false),
    AppLocale("si-rLK", "si", false),
    AppLocale("sl-rSI", "sl", false),
    AppLocale("sq-rAL", "sq", false),
    AppLocale("sr-rCS", "b+sr+Latn", false),
    AppLocale("sr-rSP", "sr", false),
    AppLocale("sv-rSE", "sv", false),
    AppLocale("sw-rKE", "sw", false),
    AppLocale("ta-rIN", "ta", false),
    AppLocale("te-rIN", "te", false),
    AppLocale("ur-rIN", "ur", false),
    AppLocale("uz-rUZ", "uz", false),
    AppLocale("zu-rZA", "zu", false),
    AppLocale("ga-rIE", "ga", false),
    AppLocale("kmr-rTR", "kmr", false),
    AppLocale("ckb-rIR", "ckb", false)
)

internal val localesAll by lazy { (localesYouTube + localesReddit).distinct() }

internal class AppLocale(
    private val srcLocale: String,
    private val destLocale: String,
    val isBuiltInLanguage: Boolean = true
) {
    fun isDefaultLocale() = srcLocale.isEmpty()

    fun getSrcLocaleFolderName() = getValuesFolderName(srcLocale)
    fun getDestLocaleFolderName() = getValuesFolderName(destLocale)

    override fun toString(): String {
        return "AppLocale(srcLocale='${getSrcLocaleFolderName()}', destLocale='${getDestLocaleFolderName()}', " +
                "isBuiltInLanguage=$isBuiltInLanguage)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppLocale

        if (srcLocale != other.srcLocale) return false
        if (destLocale != other.destLocale) return false

        return true
    }

    override fun hashCode(): Int {
        var result = srcLocale.hashCode()
        result = 31 * result + destLocale.hashCode()
        return result
    }

    companion object {
        private fun getValuesFolderName(localeName: String): String {
            val folderName = "values"

            return if (localeName.isEmpty()) {
                folderName
            } else {
                "$folderName-$localeName"
            }
        }
    }
}

private enum class BundledResourceType {
    // Add more resource XML files as needed.
    ARRAYS,
    COLORS,
    STRINGS,
    IDS;

    override fun toString(): String {
        return super.toString().lowercase(Locale.US)
    }
}

private val appsToInclude = mutableSetOf<String>()

internal lateinit var locales : List<AppLocale>

internal fun setAddResourceLocale(appLocale: List<AppLocale>) {
    locales = appLocale
}

/**
 * Add all resources for the given app.
 */
internal fun addAppResources(appId: String) {
    appsToInclude.add(appId)
}

private class DestinationFile(val file: File, val text: String) {
    val added = linkedMapOf<Pair<String, String>, String>()
}

private const val EMPTY_RESOURCES_FILE = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n</resources>"

internal val addResourcesPatch = resourcePatch(
    description = "Add resources such as strings or arrays to the app."
) {
    finalize {
        val logger = Logger.getLogger(AppLocale.Companion::class.java.name)
        val appStringsFiles = listDecodableFiles("res/values*/strings.xml").toHashSet()
        val defaultResources = mutableMapOf<String, MutableSet<String>>()

        locales.sortedBy { !it.isDefaultLocale() }.forEach { locale ->
            val stringsPath = "res/${locale.getDestLocaleFolderName()}/strings.xml"
            if (locale.isBuiltInLanguage && stringsPath !in appStringsFiles) {
                logger.warning {
                    "!!! Provided app does not contain all region localizations. " +
                        "Locale: $locale does not exist in provided app file: $stringsPath"
                }
            }

            val destinations = mutableMapOf<String, DestinationFile>()
            appsToInclude.forEach { app ->
                val appDefaults = defaultResources.getOrPut(app) { mutableSetOf() }
                BundledResourceType.entries.forEach resourceType@{ type ->
                    val srcFolder = locale.getSrcLocaleFolderName()
                    val srcPath = "$srcFolder/$app/$type.xml"
                    val stream = inputStreamFromBundledResource("addresources", srcPath)
                    if (stream == null) {
                        require(type != BundledResourceType.STRINGS) { "Could not find: $srcPath" }
                        return@resourceType
                    }

                    val source = stream.bufferedReader().use { it.readText() }
                    val destPath = "res/${locale.getDestLocaleFolderName()}/$type.xml"
                    val destination = destinations.getOrPut(destPath) {
                        val file = get(destPath, copy = false)
                        file.parentFile?.mkdirs()
                        DestinationFile(file, if (file.exists()) file.readText() else EMPTY_RESOURCES_FILE)
                    }
                    val namesAdded = mutableSetOf<String>()
                    forEachRawResourceElement(source) { element ->
                        val name = element.name
                        if (!namesAdded.add(name)) {
                            logger.warning("Duplicate string resource is declared: $srcFolder resource: $name")
                            return@forEachRawResourceElement
                        }
                        if (locale.isDefaultLocale()) {
                            appDefaults.add(name)
                        } else if (name !in appDefaults) {
                            return@forEachRawResourceElement
                        }

                        val inner = element.inner
                        val value = if (type == BundledResourceType.STRINGS && inner != null) {
                            sanitizeAndroidResourceString(name, inner, destPath)
                        } else {
                            inner
                        }
                        destination.added[element.tag to name] = if (value == null) {
                            element.openTag
                        } else {
                            "${element.openTag}$value</${element.tag}>"
                        }
                    }
                }
            }

            destinations.forEach { (path, destination) ->
                if (destination.added.isEmpty()) return@forEach
                val original = destination.text
                val text = buildString {
                    var copied = 0
                    forEachRawResourceElement(original) { element ->
                        if (element.tag to element.name in destination.added) {
                            append(original, copied, element.start)
                            copied = element.end
                        }
                    }
                    append(original, copied, original.length)
                }.replace(Regex("<resources\\s*/>"), "<resources></resources>")
                val closing = text.lastIndexOf("</resources>")
                check(closing >= 0) { "$path has no closing resources tag" }
                destination.file.writeText(buildString {
                    append(text.substring(0, closing).trimEnd()).append('\n')
                    destination.added.values.forEach { append("    ").append(it).append('\n') }
                    append(text.substring(closing))
                })
            }
        }
    }
}
