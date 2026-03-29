package com.neuralsound.mediafxkit.fx

/**
 * Output format for FX processing results.
 *
 * @param extension File extension
 * @param codec FFmpeg audio codec name
 * @param bitrate Default audio bitrate
 */
enum class OutputFormat(val extension: String, val codec: String, val bitrate: String) {
    M4A("m4a", "aac", "192k"),
    MP3("mp3", "libmp3lame", "192k"),
    WAV("wav", "pcm_s16le", "")
}
