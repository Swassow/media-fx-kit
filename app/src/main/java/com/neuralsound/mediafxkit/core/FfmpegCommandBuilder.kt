package com.neuralsound.mediafxkit.core

import com.neuralsound.mediafxkit.effects.AudioEffect
import com.neuralsound.mediafxkit.util.FilterEscapeUtils

/**
 * Fluent builder for constructing FFmpeg commands.
 */
class FfmpegCommandBuilder {
    
    private val inputs = mutableListOf<InputSpec>()
    private val filters = mutableListOf<String>()
    private val outputOptions = mutableListOf<String>()
    private var outputPath: String? = null
    private var overwrite: Boolean = true
    
    /**
     * Input file specification.
     */
    data class InputSpec(
        val path: String,
        val options: List<String> = emptyList()
    )
    
    /**
     * Add an input file.
     */
    fun input(path: String, vararg options: String): FfmpegCommandBuilder {
        inputs.add(InputSpec(path, options.toList()))
        return this
    }
    
    /**
     * Add an input file with volume adjustment.
     */
    fun inputWithVolume(path: String, volume: Float): FfmpegCommandBuilder {
        inputs.add(InputSpec(path))
        val inputIndex = inputs.size - 1
        filters.add("[$inputIndex:a]volume=$volume[a$inputIndex]")
        return this
    }
    
    /**
     * Add a raw filter string.
     */
    fun filter(filter: String): FfmpegCommandBuilder {
        filters.add(filter)
        return this
    }
    
    /**
     * Add an AudioEffect filter.
     */
    fun effect(effect: AudioEffect): FfmpegCommandBuilder {
        filters.add(effect.toFilterString())
        return this
    }
    
    /**
     * Add multiple effects.
     */
    fun effects(vararg effects: AudioEffect): FfmpegCommandBuilder {
        effects.forEach { effect(it) }
        return this
    }
    
    /**
     * Chain multiple effects together.
     */
    fun effectChain(effects: List<AudioEffect>): FfmpegCommandBuilder {
        if (effects.isNotEmpty()) {
            val chainedFilters = effects.joinToString(",") { it.toFilterString() }
            filters.add(chainedFilters)
        }
        return this
    }
    
    /**
     * Set audio codec.
     */
    fun audioCodec(codec: String): FfmpegCommandBuilder {
        outputOptions.add("-c:a")
        outputOptions.add(codec)
        return this
    }
    
    /**
     * Set audio bitrate.
     */
    fun audioBitrate(bitrate: String): FfmpegCommandBuilder {
        outputOptions.add("-b:a")
        outputOptions.add(bitrate)
        return this
    }
    
    /**
     * Set audio sample rate.
     */
    fun sampleRate(rate: Int): FfmpegCommandBuilder {
        outputOptions.add("-ar")
        outputOptions.add(rate.toString())
        return this
    }
    
    /**
     * Set audio channels.
     */
    fun channels(count: Int): FfmpegCommandBuilder {
        outputOptions.add("-ac")
        outputOptions.add(count.toString())
        return this
    }
    
    /**
     * Set output format.
     */
    fun format(format: String): FfmpegCommandBuilder {
        outputOptions.add("-f")
        outputOptions.add(format)
        return this
    }
    
    /**
     * Add custom output option.
     */
    fun outputOption(vararg options: String): FfmpegCommandBuilder {
        outputOptions.addAll(options)
        return this
    }
    
    /**
     * Set whether to overwrite output file.
     */
    fun overwrite(enabled: Boolean): FfmpegCommandBuilder {
        this.overwrite = enabled
        return this
    }
    
    /**
     * Set output file path.
     */
    fun output(path: String): FfmpegCommandBuilder {
        this.outputPath = path
        return this
    }
    
    /**
     * Build the FFmpeg command string.
     * @return Pair of command string and output path
     */
    fun build(): Pair<String, String> {
        val output = outputPath ?: throw IllegalStateException("Output path not set")
        
        val command = buildString {
            // Overwrite flag
            if (overwrite) {
                append("-y ")
            }
            
            // Input files
            inputs.forEach { input ->
                input.options.forEach { opt ->
                    append("$opt ")
                }
                append("-i ")
                append(FilterEscapeUtils.escapePath(input.path))
                append(" ")
            }
            
            // Filter complex or simple filter
            if (filters.isNotEmpty()) {
                if (inputs.size > 1 || filters.any { it.contains("[") }) {
                    // Use filter_complex for multiple inputs or complex filters
                    append("-filter_complex \"")
                    append(filters.joinToString(";"))
                    append("\" ")
                } else {
                    // Use simple audio filter
                    append("-af \"")
                    append(filters.joinToString(","))
                    append("\" ")
                }
            }
            
            // Output options
            outputOptions.forEach { opt ->
                append("$opt ")
            }
            
            // Output file
            append(FilterEscapeUtils.escapePath(output))
        }
        
        return command.trim() to output
    }
    
    /**
     * Execute the built command synchronously.
     */
    fun executeSync(): ProcessingResult {
        val (command, output) = build()
        return FfmpegExecutor.executeSync(command, output)
    }
    
    /**
     * Execute the built command asynchronously.
     */
    suspend fun executeAsync(onProgress: ((Float) -> Unit)? = null): ProcessingResult {
        val (command, output) = build()
        return FfmpegExecutor.executeAsync(command, output, onProgress)
    }
    
    companion object {
        /**
         * Create a new builder instance.
         */
        fun create(): FfmpegCommandBuilder = FfmpegCommandBuilder()
    }
}
