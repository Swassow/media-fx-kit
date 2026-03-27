package com.neuralsound.mediafxkit.operations

import com.neuralsound.mediafxkit.core.FfmpegCommandBuilder
import com.neuralsound.mediafxkit.core.ProcessingResult
import com.neuralsound.mediafxkit.util.FilterEscapeUtils
import com.neuralsound.mediafxkit.util.PathUtils

/**
 * Audio mixing operations for combining multiple audio tracks.
 */
object AudioMixer {
    
    /**
     * Track configuration for mixing.
     */
    data class Track(
        val path: String,
        val volume: Float = 1.0f,
        val delay: Float = 0f // Delay in seconds before track starts
    )
    
    /**
     * Mix multiple audio tracks into a single output.
     * 
     * @param tracks List of tracks to mix
     * @param outputPath Output file path
     * @param normalize Whether to normalize the output
     * @return ProcessingResult
     */
    fun mixSync(
        tracks: List<Track>,
        outputPath: String,
        normalize: Boolean = false
    ): ProcessingResult {
        require(tracks.isNotEmpty()) { "At least one track is required" }
        
        val command = buildMixCommand(tracks, outputPath, normalize)
        return FfmpegCommandBuilder.create()
            .also { builder ->
                tracks.forEach { track ->
                    builder.input(track.path)
                }
            }
            .filter(buildMixFilter(tracks, normalize))
            .output(outputPath)
            .executeSync()
    }
    
    /**
     * Mix multiple audio tracks asynchronously.
     */
    suspend fun mixAsync(
        tracks: List<Track>,
        outputPath: String,
        normalize: Boolean = false,
        onProgress: ((Float) -> Unit)? = null
    ): ProcessingResult {
        require(tracks.isNotEmpty()) { "At least one track is required" }
        
        return FfmpegCommandBuilder.create()
            .also { builder ->
                tracks.forEach { track ->
                    builder.input(track.path)
                }
            }
            .filter(buildMixFilter(tracks, normalize))
            .output(outputPath)
            .executeAsync(onProgress)
    }
    
    /**
     * Simple mix of two tracks.
     */
    fun mixTwoSync(
        track1: String,
        track2: String,
        outputPath: String,
        volume1: Float = 1.0f,
        volume2: Float = 1.0f
    ): ProcessingResult {
        return mixSync(
            listOf(
                Track(track1, volume1),
                Track(track2, volume2)
            ),
            outputPath
        )
    }
    
    /**
     * Overlay audio on top of another (e.g., voice over music).
     */
    fun overlaySync(
        background: String,
        foreground: String,
        outputPath: String,
        backgroundVolume: Float = 0.3f,
        foregroundVolume: Float = 1.0f
    ): ProcessingResult {
        return mixSync(
            listOf(
                Track(background, backgroundVolume),
                Track(foreground, foregroundVolume)
            ),
            outputPath
        )
    }
    
    private fun buildMixFilter(tracks: List<Track>, normalize: Boolean): String {
        val inputLabels = tracks.mapIndexed { index, track ->
            val vol = FilterEscapeUtils.formatFloat(track.volume)
            if (track.delay > 0) {
                val delay = FilterEscapeUtils.formatFloat(track.delay)
                "[$index:a]adelay=${(track.delay * 1000).toInt()}|${(track.delay * 1000).toInt()},volume=$vol[a$index]"
            } else {
                "[$index:a]volume=$vol[a$index]"
            }
        }
        
        val mixInputs = tracks.indices.joinToString("") { "[a$it]" }
        val mixFilter = "${mixInputs}amix=inputs=${tracks.size}:duration=longest"
        
        val filters = inputLabels + listOf(mixFilter + if (normalize) ",loudnorm" else "")
        
        return filters.joinToString(";")
    }
    
    private fun buildMixCommand(tracks: List<Track>, outputPath: String, normalize: Boolean): String {
        // This would be the raw command string approach
        // Currently using the builder pattern instead
        return ""
    }
}
