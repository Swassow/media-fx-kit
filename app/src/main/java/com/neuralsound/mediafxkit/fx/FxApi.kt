package com.neuralsound.mediafxkit.fx

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.neuralsound.mediafxkit.util.FilterEscapeUtils
import com.neuralsound.mediafxkit.util.PathUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

/**
 * FX API — the primary interface for the NeuralSound karaoke app.
 *
 * Provides 8 individual effect methods and 1 chain method that builds a single
 * FFmpeg `-af` filter chain (one decode → filter → encode cycle, no intermediate files).
 *
 * All methods:
 * - Accept input/output file paths
 * - Execute FFmpeg internally (caller never builds FFmpeg commands)
 * - Return [FxResult] (success with output path, or failure with error message)
 * - Are `suspend` functions safe to call from `Dispatchers.IO`
 */
object FxApi {

    // ═══════════════════════════════════════════════════════════════
    // 1. Pitch Shift
    // ═══════════════════════════════════════════════════════════════

    /**
     * Shift pitch by [semitones] without changing tempo.
     *
     * @param inputPath  Source audio file
     * @param outputPath Destination audio file
     * @param semitones  Pitch shift amount (e.g. 0.5, 2.0, -1.0)
     * @param sampleRate Audio sample rate (default 44100)
     */
    suspend fun pitchShift(
        inputPath: String,
        outputPath: String,
        semitones: Double,
        sampleRate: Int = 44100
    ): FxResult = executeSingle(
        inputPath, outputPath,
        FxEffect.PitchShift(semitones, sampleRate).toFilterString()
    )

    // ═══════════════════════════════════════════════════════════════
    // 2. Chorus
    // ═══════════════════════════════════════════════════════════════

    /**
     * Add chorus effect with configurable voice count and depth.
     */
    suspend fun addChorus(
        inputPath: String,
        outputPath: String,
        inGain: Float = 0.5f,
        outGain: Float = 0.9f,
        delays: List<Int>,
        decays: List<Float>,
        speeds: List<Float>,
        depths: List<Float>
    ): FxResult = executeSingle(
        inputPath, outputPath,
        FxEffect.Chorus(inGain, outGain, delays, decays, speeds, depths).toFilterString()
    )

    // ═══════════════════════════════════════════════════════════════
    // 3. Echo
    // ═══════════════════════════════════════════════════════════════

    /**
     * Add echo/delay effect.
     */
    suspend fun addEcho(
        inputPath: String,
        outputPath: String,
        inGain: Float = 0.8f,
        outGain: Float = 0.88f,
        delayMs: Int = 60,
        decay: Float = 0.4f
    ): FxResult = executeSingle(
        inputPath, outputPath,
        FxEffect.Echo(inGain, outGain, delayMs, decay).toFilterString()
    )

    // ═══════════════════════════════════════════════════════════════
    // 4. Flanger
    // ═══════════════════════════════════════════════════════════════

    /**
     * Add flanger modulation effect.
     */
    suspend fun addFlanger(
        inputPath: String,
        outputPath: String,
        delayMs: Float = 1f,
        depth: Float = 2f,
        speed: Float = 10f,
        width: Float = 80f,
        shape: String = "sinusoidal"
    ): FxResult = executeSingle(
        inputPath, outputPath,
        FxEffect.Flanger(delayMs, depth, speed, width, shape).toFilterString()
    )

    // ═══════════════════════════════════════════════════════════════
    // 5. Tremolo
    // ═══════════════════════════════════════════════════════════════

    /**
     * Add amplitude modulation (tremolo) effect.
     */
    suspend fun addTremolo(
        inputPath: String,
        outputPath: String,
        frequency: Double = 5.0,
        depth: Double = 0.5
    ): FxResult = executeSingle(
        inputPath, outputPath,
        FxEffect.Tremolo(frequency, depth).toFilterString()
    )

    // ═══════════════════════════════════════════════════════════════
    // 6. Compress
    // ═══════════════════════════════════════════════════════════════

    /**
     * Apply dynamic range compression.
     */
    suspend fun compress(
        inputPath: String,
        outputPath: String,
        thresholdDb: Float = -20f,
        ratio: Float = 2f,
        attackMs: Float = 5f,
        releaseMs: Float = 100f,
        makeupDb: Float = 0f
    ): FxResult = executeSingle(
        inputPath, outputPath,
        FxEffect.Compressor(thresholdDb, ratio, attackMs, releaseMs, makeupDb).toFilterString()
    )

    // ═══════════════════════════════════════════════════════════════
    // 7. High-Pass
    // ═══════════════════════════════════════════════════════════════

    /**
     * Apply high-pass filter to remove low-frequency rumble.
     */
    suspend fun highPass(
        inputPath: String,
        outputPath: String,
        cutoffHz: Int = 80
    ): FxResult = executeSingle(
        inputPath, outputPath,
        FxEffect.HighPass(cutoffHz).toFilterString()
    )

    // ═══════════════════════════════════════════════════════════════
    // 8. Loudness Normalization
    // ═══════════════════════════════════════════════════════════════

    /**
     * Apply EBU R128 loudness normalization.
     */
    suspend fun normalizeLoudness(
        inputPath: String,
        outputPath: String,
        integratedLoudness: Float = -14f,
        truePeak: Float = -1f,
        loudnessRange: Float = 11f
    ): FxResult = executeSingle(
        inputPath, outputPath,
        FxEffect.LoudnessNorm(integratedLoudness, truePeak, loudnessRange).toFilterString()
    )

    // ═══════════════════════════════════════════════════════════════
    // 9. Apply Chain ⭐
    // ═══════════════════════════════════════════════════════════════

    /**
     * Apply multiple effects in a **single FFmpeg pass** (one decode → filter → encode cycle).
     * This is the primary API the app should use for all presets.
     *
     * @param inputPath    Source audio file
     * @param outputPath   Destination audio file
     * @param effects      Ordered list of effects to apply
     * @param outputFormat Output encoding format (default M4A / AAC 192kbps)
     * @return [FxResult] with success/failure status
     */
    suspend fun applyChain(
        inputPath: String,
        outputPath: String,
        effects: List<FxEffect>,
        outputFormat: OutputFormat = OutputFormat.M4A
    ): FxResult = withContext(Dispatchers.IO) {
        // Input validation
        val inputFile = File(inputPath)
        if (!inputFile.exists()) {
            return@withContext FxResult.failure("Input file does not exist: $inputPath")
        }
        if (inputFile.length() == 0L) {
            return@withContext FxResult.failure("Input file is empty: $inputPath")
        }
        if (effects.isEmpty()) {
            return@withContext FxResult.failure("Effects list must not be empty")
        }

        // Ensure output parent directory exists
        PathUtils.ensureParentDirectory(outputPath)

        // Build filter chain
        val filterChain = FxEffect.buildFilterChain(effects)

        // Build command
        val command = buildChainCommand(inputPath, outputPath, filterChain, outputFormat)

        // Execute
        executeCommand(command, outputPath)
    }

    /**
     * Callback-based async variant of [applyChain] for non-suspend contexts.
     * Returns an [FxSession] handle for cancellation.
     */
    fun applyChainAsync(
        inputPath: String,
        outputPath: String,
        effects: List<FxEffect>,
        outputFormat: OutputFormat = OutputFormat.M4A,
        onComplete: (FxResult) -> Unit
    ): FxSession {
        // Input validation
        val inputFile = File(inputPath)
        if (!inputFile.exists()) {
            onComplete(FxResult.failure("Input file does not exist: $inputPath"))
            return FxSession(sessionId = -1)
        }
        if (inputFile.length() == 0L) {
            onComplete(FxResult.failure("Input file is empty: $inputPath"))
            return FxSession(sessionId = -1)
        }
        if (effects.isEmpty()) {
            onComplete(FxResult.failure("Effects list must not be empty"))
            return FxSession(sessionId = -1)
        }

        PathUtils.ensureParentDirectory(outputPath)

        val filterChain = FxEffect.buildFilterChain(effects)
        val command = buildChainCommand(inputPath, outputPath, filterChain, outputFormat)

        val session = FFmpegKit.executeAsync(
            command,
            { session ->
                val result = if (ReturnCode.isSuccess(session.returnCode)) {
                    FxResult.success(outputPath)
                } else if (ReturnCode.isCancel(session.returnCode)) {
                    FxResult.failure("Operation cancelled")
                } else {
                    FxResult.failure(session.failStackTrace ?: "FFmpeg error (code ${session.returnCode?.value})")
                }
                onComplete(result)
            },
            { /* log callback */ },
            { /* statistics callback */ }
        )

        return FxSession(sessionId = session.sessionId)
    }

    // ═══════════════════════════════════════════════════════════════
    // Cancellation
    // ═══════════════════════════════════════════════════════════════

    /** Cancel a specific FFmpeg session by its session ID. */
    fun cancel(sessionId: Long) {
        FFmpegKit.cancel(sessionId)
    }

    /** Cancel all running FFmpeg sessions. */
    fun cancelAll() {
        FFmpegKit.cancel()
    }

    // ═══════════════════════════════════════════════════════════════
    // Internal helpers
    // ═══════════════════════════════════════════════════════════════

    /**
     * Execute a single-effect filter against an input file.
     * Handles input validation, command building, and execution.
     */
    private suspend fun executeSingle(
        inputPath: String,
        outputPath: String,
        filterString: String
    ): FxResult = withContext(Dispatchers.IO) {
        val inputFile = File(inputPath)
        if (!inputFile.exists()) {
            return@withContext FxResult.failure("Input file does not exist: $inputPath")
        }
        if (inputFile.length() == 0L) {
            return@withContext FxResult.failure("Input file is empty: $inputPath")
        }

        PathUtils.ensureParentDirectory(outputPath)

        val command = buildString {
            append("-y ")
            append("-i ${FilterEscapeUtils.escapePath(inputPath)} ")
            append("-af \"$filterString\" ")
            append(FilterEscapeUtils.escapePath(outputPath))
        }

        executeCommand(command, outputPath)
    }

    /**
     * Build the full FFmpeg command for a chain of effects.
     */
    private fun buildChainCommand(
        inputPath: String,
        outputPath: String,
        filterChain: String,
        format: OutputFormat
    ): String = buildString {
        append("-y ")
        append("-i ${FilterEscapeUtils.escapePath(inputPath)} ")
        append("-af \"$filterChain\" ")

        // Codec & bitrate settings per output format
        append("-c:a ${format.codec} ")
        if (format.bitrate.isNotEmpty()) {
            append("-b:a ${format.bitrate} ")
        }

        // M4A-specific: enable fast-start for streaming
        if (format == OutputFormat.M4A) {
            append("-movflags +faststart ")
        }

        append(FilterEscapeUtils.escapePath(outputPath))
    }

    /**
     * Execute an FFmpeg command and return an [FxResult].
     */
    private suspend fun executeCommand(command: String, outputPath: String): FxResult =
        suspendCancellableCoroutine { continuation ->
            val session = FFmpegKit.executeAsync(
                command,
                { session ->
                    val result = if (ReturnCode.isSuccess(session.returnCode)) {
                        FxResult.success(outputPath)
                    } else if (ReturnCode.isCancel(session.returnCode)) {
                        FxResult.failure("Operation cancelled")
                    } else {
                        FxResult.failure(
                            session.failStackTrace
                                ?: "FFmpeg error (code ${session.returnCode?.value})"
                        )
                    }
                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                },
                { /* log callback */ },
                { /* statistics callback */ }
            )

            continuation.invokeOnCancellation {
                FFmpegKit.cancel(session.sessionId)
            }
        }
}
