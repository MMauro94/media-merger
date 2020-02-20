package com.github.mmauro94.media_merger.group

import com.github.mmauro94.media_merger.askOption
import com.github.mmauro94.media_merger.askString
import com.github.mmauro94.media_merger.askYesNo
import com.github.mmauro94.media_merger.menu

interface Grouper<G : Group<G>> {

    fun detectGroup(filename: String): G?

    companion object {

        fun <INFO> select(type: String, vararg providers: GroupInfoProvider<INFO>): INFO? {
            if (!askYesNo("Select $type?", true)) {
                return null
            }

            val serviceName = askOption(
                question = "Select $type info provider",
                enums = providers.map { it.service.name.toLowerCase() },
                defaultValue = providers.singleOrNull()?.service?.name?.toLowerCase() ?: ""
            )
            val provider = providers.single { it.service.name.toLowerCase() == serviceName }
            return select(type, provider) {
                select(type, *providers)
            }
        }

        private fun <INFO> select(type: String, provider: GroupInfoProvider<INFO>, reselect: () -> INFO?): INFO? {
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
            menu(
                items = results.map { it.toString() } + "-- Search again --",
                onSelection = {
                    return if (it == results.size) {
                        select(type, provider, reselect)
                    } else results[it]
                },
                exitName = "None"
            )
            return null
        }
    }
}