package com.github.mmauro94.media_merger.group

import com.github.mmauro94.media_merger.util.askString
import com.github.mmauro94.media_merger.util.askYesNo
import com.github.mmauro94.media_merger.util.menu
import com.github.mmauro94.media_merger.util.select

interface Grouper<G : Group<G>> {

    fun detectGroup(filename: String): G?

    companion object {

        fun <INFO : Any> select(type: String, vararg providers: GroupInfoProvider<INFO>): INFO? {
            if (!askYesNo("Select $type?", true)) {
                return null
            }

            val provider = select(
                question = "Select $type info provider",
                options = providers.asList(),
                defaultValue = providers.singleOrNull(),
                nameProvider = { it.service.name.toLowerCase() }
            )
            return select(type, provider) {
                select(type, *providers)
            }
        }

        private fun <INFO : Any> select(type: String, provider: GroupInfoProvider<INFO>, reselect: () -> INFO?): INFO? {
            val q = askString("Name of $type to search:")
            val results = try {
                provider.search(q)
            } catch (e: GroupInfoException) {
                System.err.println("Error searching for show")
                if (e.message != null && e.message.isNotBlank()) {
                    System.err.println(e.message)
                }
                return reselect()
            }
            return menu(
                title = "Select $type",
                items = results.map { it.toString() to { it } }
                        + Pair("-- Search again --", { select(type, provider, reselect) })
                        + Pair("-- Select no show --", { null })
            )
        }
    }
}