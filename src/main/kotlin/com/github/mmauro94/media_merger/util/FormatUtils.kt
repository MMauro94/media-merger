package com.github.mmauro94.media_merger.util

private val PATTERN = buildString {
    append("\\\\(.)") //Treat any character after a backslash literally
    append('|') //OR
    append("%\\(([^)]+)\\)") //Look for %(keys) to replace
}.toRegex()


fun String.namedFormat(replacementMap: Map<String, Any>): String {
    return PATTERN.replace(this) { match ->
        val literal = match.groups[1]?.value
        if (literal != null) literal
        else {
            val replacementName = match.groupValues[2]
            val zeroPad = replacementName.takeLastWhile { it == '0' }.length

            val replacement = replacementMap[replacementName.substring(0, replacementName.length - zeroPad)]
                ?: throw IllegalStateException("""Invalid replacement name "$replacementName"""")

            replacement.toString().padStart(zeroPad, '0')
        }
    }
}