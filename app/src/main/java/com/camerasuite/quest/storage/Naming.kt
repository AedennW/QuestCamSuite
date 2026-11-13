package com.camerasuite.quest.storage

/**
 * Pure-Kotlin file-name template engine (unit-tested on the JVM).
 *
 * Supported tokens: {date} {time} {mode} {counter}
 * Example template: QCS_{mode}_{date}_{time}_{counter}
 */
object FileNamer {

    const val DEFAULT_TEMPLATE = "QCS_{mode}_{date}_{time}_{counter}"

    data class Vars(
        val mode: String,
        val ext: String,
        val date: String,      // yyyy-MM-dd
        val time: String,      // HH-mm-ss
        val counter: Int
    )

    fun render(template: String, vars: Vars): String {
        var out = template.ifBlank { DEFAULT_TEMPLATE }
        out = out.replace("{date}", vars.date)
            .replace("{time}", vars.time)
            .replace("{mode}", vars.mode.lowercase())
            .replace("{counter}", vars.counter.toString().padStart(3, '0'))

        // Storage-safe characters only (SAF providers can be picky).
        out = out.replace(Regex("[^A-Za-z0-9._-]"), "_")
        while (out.startsWith('.')) out = out.drop(1)
        if (out.isBlank()) out = "capture"

        val suffix = "." + vars.ext.lowercase()
        if (!out.lowercase().endsWith(suffix)) out += suffix
        return out
    }
}
