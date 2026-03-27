package com.neuralsound.mediafxkit.util

/**
 * Utility functions for escaping FFmpeg filter strings and paths.
 */
object FilterEscapeUtils {
    
    /**
     * Escape special characters in FFmpeg filter values.
     * FFmpeg filter syntax requires escaping: \ ' : [ ] ; ,
     */
    fun escapeFilterValue(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace(":", "\\:")
            .replace("[", "\\[")
            .replace("]", "\\]")
            .replace(";", "\\;")
            .replace(",", "\\,")
    }
    
    /**
     * Escape a file path for use in FFmpeg commands.
     * Wraps path in quotes and escapes internal quotes.
     */
    fun escapePath(path: String): String {
        val escaped = path.replace("\"", "\\\"")
        return "\"$escaped\""
    }
    
    /**
     * Format a float value for FFmpeg filters.
     * Ensures proper decimal formatting regardless of locale.
     */
    fun formatFloat(value: Float, decimals: Int = 2): String {
        return String.format(java.util.Locale.US, "%.${decimals}f", value)
    }
    
    /**
     * Format a double value for FFmpeg filters.
     */
    fun formatDouble(value: Double, decimals: Int = 2): String {
        return String.format(java.util.Locale.US, "%.${decimals}f", value)
    }
    
    /**
     * Build a filter parameter string from key-value pairs.
     */
    fun buildFilterParams(vararg params: Pair<String, Any>): String {
        return params
            .filter { (_, value) -> 
                when (value) {
                    is Boolean -> value
                    is Number -> true
                    is String -> value.isNotEmpty()
                    else -> true
                }
            }
            .joinToString(":") { (key, value) ->
                when (value) {
                    is Float -> "$key=${formatFloat(value)}"
                    is Double -> "$key=${formatDouble(value)}"
                    is Boolean -> if (value) key else ""
                    else -> "$key=$value"
                }
            }
            .replace("::", ":")
            .trimEnd(':')
    }
}
