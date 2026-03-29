package com.neuralsound.mediafxkit

import com.neuralsound.mediafxkit.core.FfmpegCommandBuilder
import com.neuralsound.mediafxkit.core.FfmpegExecutor
import com.neuralsound.mediafxkit.core.ProcessingResult
import com.neuralsound.mediafxkit.effects.*
import com.neuralsound.mediafxkit.fx.FxApi
import com.neuralsound.mediafxkit.fx.FxEffect
import com.neuralsound.mediafxkit.fx.FxResult
import com.neuralsound.mediafxkit.fx.FxSession
import com.neuralsound.mediafxkit.fx.OutputFormat
import com.neuralsound.mediafxkit.operations.*
import com.neuralsound.mediafxkit.util.PathUtils

/**
 * Media FX Kit - Main entry point for audio processing with FFmpeg.
 * 
 * Provides a simplified API for common audio operations including:
 * - Audio effects (reverb, EQ, compression, pitch shift, tempo change)
 * - Audio mixing (multi-track, overlay)
 * - Format conversion (MP3, AAC, WAV, FLAC, etc.)
 * - Volume normalization and limiting
 * - Waveform extraction for visualization
 * 
 * Example usage:
 * ```kotlin
 * // Apply reverb effect
 * MediaFxKit.applyEffect(inputPath, outputPath, Reverb.mediumHall())
 * 
 * // Chain multiple effects
 * MediaFxKit.applyEffects(inputPath, outputPath, listOf(
 *     Equalizer.bassBoost(4f),
 *     Compressor.medium(),
 *     Reverb.lightRoom()
 * ))
 * 
 * // Convert to MP3
 * MediaFxKit.convert(inputPath, outputPath, AudioConverter.Format.MP3)
 * 
 * // Mix tracks
 * MediaFxKit.mix(listOf(track1, track2), outputPath)
 * ```
 */
object MediaFxKit {
    
    // ==================== Effects ====================
    
    /**
     * Apply a single audio effect to a file.
     */
    fun applyEffect(
        inputPath: String,
        outputPath: String,
        effect: AudioEffect
    ): ProcessingResult {
        return FfmpegCommandBuilder.create()
            .input(inputPath)
            .effect(effect)
            .output(outputPath)
            .executeSync()
    }
    
    /**
     * Apply a single effect asynchronously.
     */
    suspend fun applyEffectAsync(
        inputPath: String,
        outputPath: String,
        effect: AudioEffect,
        onProgress: ((Float) -> Unit)? = null
    ): ProcessingResult {
        return FfmpegCommandBuilder.create()
            .input(inputPath)
            .effect(effect)
            .output(outputPath)
            .executeAsync(onProgress)
    }
    
    /**
     * Apply multiple effects in sequence.
     */
    fun applyEffects(
        inputPath: String,
        outputPath: String,
        effects: List<AudioEffect>
    ): ProcessingResult {
        return FfmpegCommandBuilder.create()
            .input(inputPath)
            .effectChain(effects)
            .output(outputPath)
            .executeSync()
    }
    
    /**
     * Apply multiple effects asynchronously.
     */
    suspend fun applyEffectsAsync(
        inputPath: String,
        outputPath: String,
        effects: List<AudioEffect>,
        onProgress: ((Float) -> Unit)? = null
    ): ProcessingResult {
        return FfmpegCommandBuilder.create()
            .input(inputPath)
            .effectChain(effects)
            .output(outputPath)
            .executeAsync(onProgress)
    }
    
    // ==================== Reverb ====================
    
    /**
     * Add reverb effect with preset.
     */
    fun addReverb(
        inputPath: String,
        outputPath: String,
        preset: Reverb = Reverb.mediumHall()
    ): ProcessingResult = applyEffect(inputPath, outputPath, preset)
    
    // ==================== Equalizer ====================
    
    /**
     * Apply equalizer effect.
     */
    fun equalize(
        inputPath: String,
        outputPath: String,
        eq: Equalizer
    ): ProcessingResult = applyEffect(inputPath, outputPath, eq)
    
    /**
     * Boost bass frequencies.
     */
    fun boostBass(
        inputPath: String,
        outputPath: String,
        amount: Float = 6f
    ): ProcessingResult = applyEffect(inputPath, outputPath, Equalizer.bassBoost(amount))
    
    /**
     * Boost treble frequencies.
     */
    fun boostTreble(
        inputPath: String,
        outputPath: String,
        amount: Float = 5f
    ): ProcessingResult = applyEffect(inputPath, outputPath, Equalizer.trebleBoost(amount))
    
    // ==================== Compression ====================
    
    /**
     * Apply compression with preset.
     */
    fun compress(
        inputPath: String,
        outputPath: String,
        preset: Compressor = Compressor.medium()
    ): ProcessingResult = applyEffect(inputPath, outputPath, preset)
    
    // ==================== Pitch & Tempo ====================
    
    /**
     * Shift pitch by semitones.
     */
    fun shiftPitch(
        inputPath: String,
        outputPath: String,
        semitones: Float
    ): ProcessingResult = applyEffect(inputPath, outputPath, PitchShift(semitones))
    
    /**
     * Change tempo without affecting pitch.
     */
    fun changeTempo(
        inputPath: String,
        outputPath: String,
        factor: Float
    ): ProcessingResult = applyEffect(inputPath, outputPath, TempoChange(factor))
    
    // ==================== Mixing ====================
    
    /**
     * Mix multiple audio tracks.
     */
    fun mix(
        tracks: List<AudioMixer.Track>,
        outputPath: String,
        normalize: Boolean = false
    ): ProcessingResult = AudioMixer.mixSync(tracks, outputPath, normalize)
    
    /**
     * Mix two audio files with specified volumes.
     */
    fun mixTwo(
        track1: String,
        track2: String,
        outputPath: String,
        volume1: Float = 1.0f,
        volume2: Float = 1.0f
    ): ProcessingResult = AudioMixer.mixTwoSync(track1, track2, outputPath, volume1, volume2)
    
    /**
     * Overlay audio (e.g., voice over music).
     */
    fun overlay(
        background: String,
        foreground: String,
        outputPath: String,
        backgroundVolume: Float = 0.3f,
        foregroundVolume: Float = 1.0f
    ): ProcessingResult = AudioMixer.overlaySync(background, foreground, outputPath, backgroundVolume, foregroundVolume)
    
    // ==================== Conversion ====================
    
    /**
     * Convert audio to a different format.
     */
    fun convert(
        inputPath: String,
        outputPath: String,
        format: AudioConverter.Format,
        quality: AudioConverter.Quality = AudioConverter.Quality.HIGH
    ): ProcessingResult = AudioConverter.convertSync(inputPath, outputPath, format, quality)
    
    /**
     * Convert to MP3.
     */
    fun toMp3(
        inputPath: String,
        outputPath: String = PathUtils.changeExtension(inputPath, "mp3"),
        quality: AudioConverter.Quality = AudioConverter.Quality.HIGH
    ): ProcessingResult = AudioConverter.toMp3Sync(inputPath, outputPath, quality)
    
    /**
     * Convert to WAV.
     */
    fun toWav(
        inputPath: String,
        outputPath: String = PathUtils.changeExtension(inputPath, "wav")
    ): ProcessingResult = AudioConverter.toWavSync(inputPath, outputPath)
    
    /**
     * Extract audio from video.
     */
    fun extractAudio(
        videoPath: String,
        outputPath: String,
        format: AudioConverter.Format = AudioConverter.Format.MP3
    ): ProcessingResult = AudioConverter.extractAudioSync(videoPath, outputPath, format)
    
    // ==================== Normalization ====================
    
    /**
     * Normalize audio loudness.
     */
    fun normalize(
        inputPath: String,
        outputPath: String,
        targetLufs: Float = -14f
    ): ProcessingResult = VolumeNormalizer.normalizeSync(inputPath, outputPath, targetLufs)
    
    /**
     * Normalize to a standard target.
     */
    fun normalizeTo(
        inputPath: String,
        outputPath: String,
        target: VolumeNormalizer.LoudnessTarget
    ): ProcessingResult = VolumeNormalizer.normalizeToTargetSync(inputPath, outputPath, target)
    
    /**
     * Adjust volume by dB.
     */
    fun adjustVolume(
        inputPath: String,
        outputPath: String,
        volumeDb: Float
    ): ProcessingResult = VolumeNormalizer.adjustVolumeSync(inputPath, outputPath, volumeDb)
    
    /**
     * Apply limiting to prevent clipping.
     */
    fun limit(
        inputPath: String,
        outputPath: String,
        ceiling: Float = -0.3f
    ): ProcessingResult = VolumeNormalizer.limitSync(inputPath, outputPath, ceiling)
    
    // ==================== Waveform ====================
    
    /**
     * Extract waveform data for visualization.
     */
    suspend fun extractWaveform(
        inputPath: String,
        samplesPerSecond: Int = 10
    ): WaveformExtractor.WaveformData? = WaveformExtractor.extract(inputPath, samplesPerSecond)
    
    /**
     * Extract simplified peak data for visualization.
     */
    suspend fun extractPeaks(
        inputPath: String,
        numPeaks: Int = 100
    ): FloatArray? = WaveformExtractor.extractPeaks(inputPath, numPeaks)
    
    // ==================== Command Builder ====================
    
    /**
     * Create a custom command builder for advanced use cases.
     */
    fun command(): FfmpegCommandBuilder = FfmpegCommandBuilder.create()
    
    // ==================== FX API (Karaoke App Interface) ====================
    
    /**
     * Shift pitch by N semitones without changing tempo.
     * Used by: Hard Tune, Natural Tune, Super Vocoder presets.
     */
    suspend fun pitchShiftFx(
        inputPath: String,
        outputPath: String,
        semitones: Double,
        sampleRate: Int = 44100
    ): FxResult = FxApi.pitchShift(inputPath, outputPath, semitones, sampleRate)
    
    /**
     * Add chorus effect with configurable voice count and depth.
     * Used by: Hard Tune (single voice), Big Chorus (3 voices).
     */
    suspend fun addChorusFx(
        inputPath: String,
        outputPath: String,
        inGain: Float = 0.5f,
        outGain: Float = 0.9f,
        delays: List<Int>,
        decays: List<Float>,
        speeds: List<Float>,
        depths: List<Float>
    ): FxResult = FxApi.addChorus(inputPath, outputPath, inGain, outGain, delays, decays, speeds, depths)
    
    /**
     * Add echo/delay effect.
     * Used by: Natural Tune, Super Vocoder.
     */
    suspend fun addEchoFx(
        inputPath: String,
        outputPath: String,
        inGain: Float = 0.8f,
        outGain: Float = 0.88f,
        delayMs: Int = 60,
        decay: Float = 0.4f
    ): FxResult = FxApi.addEcho(inputPath, outputPath, inGain, outGain, delayMs, decay)
    
    /**
     * Add flanger modulation effect.
     * Used by: Super Vocoder.
     */
    suspend fun addFlangerFx(
        inputPath: String,
        outputPath: String,
        delayMs: Float = 1f,
        depth: Float = 2f,
        speed: Float = 10f,
        width: Float = 80f,
        shape: String = "sinusoidal"
    ): FxResult = FxApi.addFlanger(inputPath, outputPath, delayMs, depth, speed, width, shape)
    
    /**
     * Add amplitude modulation (tremolo) effect.
     * Used by: Super Vocoder.
     */
    suspend fun addTremoloFx(
        inputPath: String,
        outputPath: String,
        frequency: Double = 5.0,
        depth: Double = 0.5
    ): FxResult = FxApi.addTremolo(inputPath, outputPath, frequency, depth)
    
    /**
     * Apply dynamic range compression.
     * Used by: Hard Tune, Clean.
     */
    suspend fun compressFx(
        inputPath: String,
        outputPath: String,
        thresholdDb: Float = -20f,
        ratio: Float = 2f,
        attackMs: Float = 5f,
        releaseMs: Float = 100f,
        makeupDb: Float = 0f
    ): FxResult = FxApi.compress(inputPath, outputPath, thresholdDb, ratio, attackMs, releaseMs, makeupDb)
    
    /**
     * Apply high-pass filter to remove low-frequency rumble.
     * Used by: Clean.
     */
    suspend fun highPassFx(
        inputPath: String,
        outputPath: String,
        cutoffHz: Int = 80
    ): FxResult = FxApi.highPass(inputPath, outputPath, cutoffHz)
    
    /**
     * Apply EBU R128 loudness normalization.
     * Used by: Clean.
     */
    suspend fun normalizeLoudnessFx(
        inputPath: String,
        outputPath: String,
        integratedLoudness: Float = -14f,
        truePeak: Float = -1f,
        loudnessRange: Float = 11f
    ): FxResult = FxApi.normalizeLoudness(inputPath, outputPath, integratedLoudness, truePeak, loudnessRange)
    
    /**
     * Apply multiple effects in a single FFmpeg pass (one decode → filter → encode cycle).
     * This is the primary API for all karaoke FX presets.
     *
     * Example (Hard Tune at strength 54):
     * ```kotlin
     * MediaFxKit.applyChain(
     *     inputPath = "/path/to/vocal.m4a",
     *     outputPath = "/path/to/output.m4a",
     *     effects = listOf(
     *         FxEffect.PitchShift(semitones = 1.08),
     *         FxEffect.Chorus(delays = listOf(55), decays = listOf(0.4f), speeds = listOf(0.25f), depths = listOf(2f)),
     *         FxEffect.Compressor(thresholdDb = -15f, ratio = 3f, attackMs = 5f, releaseMs = 50f)
     *     )
     * )
     * ```
     */
    suspend fun applyChain(
        inputPath: String,
        outputPath: String,
        effects: List<FxEffect>,
        outputFormat: OutputFormat = OutputFormat.M4A
    ): FxResult = FxApi.applyChain(inputPath, outputPath, effects, outputFormat)
    
    /**
     * Callback-based async variant of [applyChain] for non-suspend contexts.
     * Returns an [FxSession] handle that can be passed to [cancel] for cancellation.
     */
    fun applyChainAsync(
        inputPath: String,
        outputPath: String,
        effects: List<FxEffect>,
        outputFormat: OutputFormat = OutputFormat.M4A,
        onComplete: (FxResult) -> Unit
    ): FxSession = FxApi.applyChainAsync(inputPath, outputPath, effects, outputFormat, onComplete)
    
    // ==================== Control ====================
    
    /**
     * Cancel the currently running operation.
     */
    fun cancel() = FfmpegExecutor.cancel()
    
    /**
     * Cancel a specific FFmpeg session by its session ID.
     */
    fun cancel(sessionId: Long) = FfmpegExecutor.cancel(sessionId)
    
    /**
     * Cancel all running operations.
     */
    fun cancelAll() = FfmpegExecutor.cancelAll()
}
