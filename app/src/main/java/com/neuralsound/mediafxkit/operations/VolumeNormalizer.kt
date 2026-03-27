package com.neuralsound.mediafxkit.operations

import com.neuralsound.mediafxkit.core.FfmpegCommandBuilder
import com.neuralsound.mediafxkit.core.ProcessingResult
import com.neuralsound.mediafxkit.util.FilterEscapeUtils

/**
 * Volume normalization and loudness operations.
 */
object VolumeNormalizer {
    
    /**
     * Loudness normalization targets based on different standards.
     */
    enum class LoudnessTarget(val lufs: Float) {
        BROADCAST(-24f),      // EBU R128 broadcast standard
        STREAMING(-14f),      // Spotify, YouTube, etc.
        PODCAST(-16f),        // Podcast standard
        MUSIC_MASTER(-9f),    // Music mastering target
        CUSTOM(0f)            // Custom target (use parameter)
    }
    
    /**
     * Normalize audio using EBU R128 loudnorm filter.
     * 
     * @param inputPath Input file path
     * @param outputPath Output file path
     * @param targetLufs Target loudness in LUFS
     * @param truePeak Maximum true peak level in dBTP
     * @param lra Loudness range target
     * @return ProcessingResult
     */
    fun normalizeSync(
        inputPath: String,
        outputPath: String,
        targetLufs: Float = -14f,
        truePeak: Float = -1f,
        lra: Float = 7f
    ): ProcessingResult {
        val params = buildString {
            append("I=${FilterEscapeUtils.formatFloat(targetLufs)}")
            append(":TP=${FilterEscapeUtils.formatFloat(truePeak)}")
            append(":LRA=${FilterEscapeUtils.formatFloat(lra)}")
        }
        
        return FfmpegCommandBuilder.create()
            .input(inputPath)
            .filter("loudnorm=$params")
            .output(outputPath)
            .executeSync()
    }
    
    /**
     * Normalize audio asynchronously.
     */
    suspend fun normalizeAsync(
        inputPath: String,
        outputPath: String,
        targetLufs: Float = -14f,
        truePeak: Float = -1f,
        lra: Float = 7f,
        onProgress: ((Float) -> Unit)? = null
    ): ProcessingResult {
        val params = buildString {
            append("I=${FilterEscapeUtils.formatFloat(targetLufs)}")
            append(":TP=${FilterEscapeUtils.formatFloat(truePeak)}")
            append(":LRA=${FilterEscapeUtils.formatFloat(lra)}")
        }
        
        return FfmpegCommandBuilder.create()
            .input(inputPath)
            .filter("loudnorm=$params")
            .output(outputPath)
            .executeAsync(onProgress)
    }
    
    /**
     * Normalize to a preset target.
     */
    fun normalizeToTargetSync(
        inputPath: String,
        outputPath: String,
        target: LoudnessTarget
    ): ProcessingResult {
        return normalizeSync(inputPath, outputPath, target.lufs)
    }
    
    /**
     * Apply simple volume adjustment.
     * 
     * @param inputPath Input file path
     * @param outputPath Output file path
     * @param volumeDb Volume adjustment in dB (positive = louder, negative = quieter)
     * @return ProcessingResult
     */
    fun adjustVolumeSync(
        inputPath: String,
        outputPath: String,
        volumeDb: Float
    ): ProcessingResult {
        val vol = FilterEscapeUtils.formatFloat(volumeDb)
        return FfmpegCommandBuilder.create()
            .input(inputPath)
            .filter("volume=${vol}dB")
            .output(outputPath)
            .executeSync()
    }
    
    /**
     * Apply volume adjustment by multiplier (1.0 = no change).
     */
    fun adjustVolumeMultiplierSync(
        inputPath: String,
        outputPath: String,
        multiplier: Float
    ): ProcessingResult {
        return FfmpegCommandBuilder.create()
            .input(inputPath)
            .filter("volume=${FilterEscapeUtils.formatFloat(multiplier)}")
            .output(outputPath)
            .executeSync()
    }
    
    /**
     * Apply peak limiting to prevent clipping.
     * 
     * @param inputPath Input file path
     * @param outputPath Output file path
     * @param ceiling Maximum level in dB (typically -0.1 to -1.0)
     * @param release Release time in seconds
     * @return ProcessingResult
     */
    fun limitSync(
        inputPath: String,
        outputPath: String,
        ceiling: Float = -0.3f,
        release: Float = 0.05f
    ): ProcessingResult {
        val params = buildString {
            append("limit=${FilterEscapeUtils.formatFloat(ceiling)}dB")
            append(":release=${FilterEscapeUtils.formatFloat(release)}")
        }
        
        return FfmpegCommandBuilder.create()
            .input(inputPath)
            .filter("alimiter=$params")
            .output(outputPath)
            .executeSync()
    }
    
    /**
     * Normalize and limit in one operation (mastering chain).
     */
    fun masterSync(
        inputPath: String,
        outputPath: String,
        targetLufs: Float = -14f,
        truePeak: Float = -1f
    ): ProcessingResult {
        val loudnormParams = "I=${FilterEscapeUtils.formatFloat(targetLufs)}:TP=${FilterEscapeUtils.formatFloat(truePeak)}"
        
        return FfmpegCommandBuilder.create()
            .input(inputPath)
            .filter("loudnorm=$loudnormParams,alimiter=limit=${FilterEscapeUtils.formatFloat(truePeak)}dB")
            .output(outputPath)
            .executeSync()
    }
}
