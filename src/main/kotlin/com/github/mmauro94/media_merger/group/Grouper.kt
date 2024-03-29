package com.github.mmauro94.media_merger.group

import com.github.mmauro94.media_merger.InputFiles
import com.github.mmauro94.media_merger.util.Reporter
import com.github.mmauro94.media_merger.util.cli.type.BooleanCliType
import com.github.mmauro94.media_merger.util.cli.type.StringCliType
import com.github.mmauro94.media_merger.util.filterNullKeys
import com.github.mmauro94.media_merger.util.log.Logger
import com.github.mmauro94.media_merger.util.menu
import com.github.mmauro94.media_merger.util.select
import org.fusesource.jansi.Ansi.ansi
import java.io.File

interface Grouper<G : Group<G>> {

    fun detectGroup(filename: String, logger: Logger): G?

    fun detectGroups(dir: File, reporter: Reporter): Map<G, List<File>> {
        reporter.progress.indeterminate("Listing files...")
        val allFiles = dir.walkTopDown().toList()
        reporter.progress.indeterminate("Grouping files...")
        return allFiles
            .filter { it.extension in InputFiles.EXTENSIONS_TO_IDENTIFY }
            .filterNot { it.name.contains("@adjusted") || it.name.contains("@extracted") }
            .groupBy { detectGroup(it.name, reporter.log) }
            .filterNullKeys()
    }

    companion object {

        fun <INFO : Any> select(type: String, vararg providers: GroupInfoProvider<INFO>): INFO? {
            if (!BooleanCliType.ask("Select $type?", true)) {
                return null
            }

            val provider = select(
                question = "Select $type info provider",
                options = providers.asList(),
                defaultValue = providers.singleOrNull(),
                nameProvider = { it.service.name.lowercase() }
            )
            return select(type, provider) {
                select(type, *providers)
            }
        }

        private fun <INFO : Any> select(type: String, provider: GroupInfoProvider<INFO>, reselect: () -> INFO?): INFO? {
            val q = StringCliType.ask("Name of $type to search:")
            val results = try {
                provider.search(q)
            } catch (e: GroupInfoException) {
                print(ansi().fgRed().a("Error searching for $type: ${e.message}"))
                println(ansi().reset())
                return reselect()
            }
            return menu(
                title = "Select $type",
                items = results.map { it.toString() to { it } }
                        + Pair<String, () -> INFO?>("-- Search again --", { select(type, provider, reselect) })
                        + Pair("-- Select no show --", { null })
            )
        }
    }
}