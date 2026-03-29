package com.neuralsound.mediafxkit.fx

import com.neuralsound.mediafxkit.util.FilterEscapeUtils
import java.util.Locale

/**
 * Sealed class representing effect configurations for [FxApi.applyChain].
 *
 * Each subtype carries its parameters and can produce the corresponding FFmpeg
 * audio filter string via [toFilterString].
 *
 * Example:
 * ```kotlin
 * val effects = listOf(
 *     FxEffect.PitchShift(semitones = 1.08),
 *     FxEffect.Chorus(delays = listOf(55), decays = listOf(0.4f), speeds = listOf(0.25f), depths = listOf(2f)),
 *     FxEffect.Compressor(thresholdDb = -15f, ratio = 3f)
 * )
 * ```
 */
sealed class FxEffect {

    /** Produce the FFmpeg `-af` filter string segment for this effect. */
    abstract fun toFilterString(): String

    // ────────────────────────────────────────────────────────────────
    // 1. Pitch Shift
    // ────────────────────────────────────────────────────────────────

    /**
     * Shift pitch by [semitones] without changing tempo.
     *
     * FFmpeg filter: `asetrate=SR*factor,aresample=SR,atempo=1/factor`
     */
    data class PitchShift(
        val semitones: Double,
        val sampleRate: Int = 44100
    ) : FxEffect() {
        override fun toFilterString(): String {
            val factor = Math.pow(2.0, semitones / 12.0)
            val mult = fmt(factor, 6)
            val tempoComp = fmt(1.0 / factor, 6)
            return "asetrate=$sampleRate*$mult,aresample=$sampleRate,atempo=$tempoComp"
        }
    }

    // ────────────────────────────────────────────────────────────────
    // 2. Chorus
    // ────────────────────────────────────────────────────────────────

    /**
     * Chorus effect with configurable voice count and depth.
     *
     * FFmpeg filter: `chorus=inGain:outGain:d1|d2:dec1|dec2:spd1|spd2:dep1|dep2`
     */
    data class Chorus(
        val inGain: Float = 0.5f,
        val outGain: Float = 0.9f,
        val delays: List<Int>,
        val decays: List<Float>,
        val speeds: List<Float>,
        val depths: List<Float>
    ) : FxEffect() {
        override fun toFilterString(): String {
            val ig = fmtF(inGain)
            val og = fmtF(outGain)
            val d = delays.joinToString("|")
            val dec = decays.joinToString("|") { fmtF(it) }
            val spd = speeds.joinToString("|") { fmtF(it) }
            val dep = depths.joinToString("|") { fmtF(it) }
            return "chorus=$ig:$og:$d:$dec:$spd:$dep"
        }
    }

    // ────────────────────────────────────────────────────────────────
    // 3. Echo
    // ────────────────────────────────────────────────────────────────

    /**
     * Echo/delay effect.
     *
     * FFmpeg filter: `aecho=inGain:outGain:delayMs:decay`
     */
    data class Echo(
        val inGain: Float = 0.8f,
        val outGain: Float = 0.88f,
        val delayMs: Int = 60,
        val decay: Float = 0.4f
    ) : FxEffect() {
        override fun toFilterString(): String {
            return "aecho=${fmtF(inGain)}:${fmtF(outGain)}:$delayMs:${fmtF(decay)}"
        }
    }

    // ────────────────────────────────────────────────────────────────
    // 4. Flanger
    // ────────────────────────────────────────────────────────────────

    /**
     * Flanger modulation effect.
     *
     * FFmpeg filter: `flanger=delay=D:depth=DEP:speed=S:width=W:shape=SHAPE`
     */
    data class Flanger(
        val delayMs: Float = 1f,
        val depth: Float = 2f,
        val speed: Float = 10f,
        val width: Float = 80f,
        val shape: String = "sinusoidal"
    ) : FxEffect() {
        override fun toFilterString(): String {
            return "flanger=delay=${fmtF(delayMs)}:depth=${fmtF(depth)}" +
                    ":speed=${fmtF(speed)}:width=${fmtF(width)}:shape=$shape"
        }
    }

    // ────────────────────────────────────────────────────────────────
    // 5. Tremolo
    // ────────────────────────────────────────────────────────────────

    /**
     * Amplitude modulation (tremolo) effect.
     *
     * FFmpeg filter: `tremolo=f=FREQ:d=DEPTH`
     */
    data class Tremolo(
        val frequency: Double = 5.0,
        val depth: Double = 0.5
    ) : FxEffect() {
        override fun toFilterString(): String {
            return "tremolo=f=${fmt(frequency)}:d=${fmt(depth)}"
        }
    }

    // ────────────────────────────────────────────────────────────────
    // 6. Compressor
    // ────────────────────────────────────────────────────────────────

    /**
     * Dynamic range compressor.
     *
     * FFmpeg filter: `acompressor=threshold=TdB:ratio=R:attack=A:release=REL:makeup=MdB`
     */
    data class Compressor(
        val thresholdDb: Float = -20f,
        val ratio: Float = 2f,
        val attackMs: Float = 5f,
        val releaseMs: Float = 100f,
        val makeupDb: Float = 0f
    ) : FxEffect() {
        override fun toFilterString(): String {
            val params = buildString {
                append("threshold=${fmtF(thresholdDb)}dB")
                append(":ratio=${fmtF(ratio)}")
                append(":attack=${fmtF(attackMs)}")
                append(":release=${fmtF(releaseMs)}")
                if (makeupDb != 0f) {
                    append(":makeup=${fmtF(makeupDb)}dB")
                }
            }
            return "acompressor=$params"
        }
    }

    // ────────────────────────────────────────────────────────────────
    // 7. High-Pass Filter
    // ────────────────────────────────────────────────────────────────

    /**
     * High-pass filter to remove low-frequency rumble.
     *
     * FFmpeg filter: `highpass=f=CUTOFF`
     */
    data class HighPass(
        val cutoffHz: Int = 80
    ) : FxEffect() {
        override fun toFilterString(): String = "highpass=f=$cutoffHz"
    }

    // ────────────────────────────────────────────────────────────────
    // 8. Loudness Normalization
    // ────────────────────────────────────────────────────────────────

    /**
     * EBU R128 loudness normalization.
     *
     * FFmpeg filter: `loudnorm=I=IL:TP=TP:LRA=LRA`
     */
    data class LoudnessNorm(
        val integratedLoudness: Float = -14f,
        val truePeak: Float = -1f,
        val loudnessRange: Float = 11f
    ) : FxEffect() {
        override fun toFilterString(): String {
            return "loudnorm=I=${fmtF(integratedLoudness)}:TP=${fmtF(truePeak)}:LRA=${fmtF(loudnessRange)}"
        }
    }

    // ────────────────────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────────────────────

    companion object {
        /** Build a comma-separated `-af` filter chain from a list of effects. */
        fun buildFilterChain(effects: List<FxEffect>): String {
            return effects.joinToString(",") { it.toFilterString() }
        }

        private fun fmtF(v: Float, dec: Int = 2): String =
            String.format(Locale.US, "%.${dec}f", v)

        private fun fmt(v: Double, dec: Int = 2): String =
            String.format(Locale.US, "%.${dec}f", v)
    }
}

// Extension so subclasses can access formatting helpers
private fun fmtF(v: Float, dec: Int = 2): String =
    String.format(Locale.US, "%.${dec}f", v)

private fun fmt(v: Double, dec: Int = 2): String =
    String.format(Locale.US, "%.${dec}f", v)
