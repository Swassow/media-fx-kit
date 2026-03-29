package com.neuralsound.mediafxkit.effects

import com.neuralsound.mediafxkit.util.FilterEscapeUtils

/**
 * EBU R128 loudness normalization effect using FFmpeg's loudnorm filter.
 *
 * @param integratedLoudness Target integrated loudness in LUFS
 * @param truePeak Maximum true peak level in dBTP
 * @param loudnessRange Target loudness range in LU
 */
data class LoudnessNorm(
    val integratedLoudness: Float = -14f,
    val truePeak: Float = -1f,
    val loudnessRange: Float = 11f
) : AudioEffect() {

    override fun toFilterString(): String {
        val i = FilterEscapeUtils.formatFloat(integratedLoudness)
        val tp = FilterEscapeUtils.formatFloat(truePeak)
        val lra = FilterEscapeUtils.formatFloat(loudnessRange)
        return "loudnorm=I=$i:TP=$tp:LRA=$lra"
    }

    override fun description(): String = "Loudness Norm (${integratedLoudness} LUFS)"

    companion object {
        /** Streaming standard (-14 LUFS). */
        fun streaming() = LoudnessNorm(integratedLoudness = -14f, truePeak = -1f, loudnessRange = 11f)

        /** Broadcast standard (-24 LUFS). */
        fun broadcast() = LoudnessNorm(integratedLoudness = -24f, truePeak = -2f, loudnessRange = 7f)

        /** Podcast standard (-16 LUFS). */
        fun podcast() = LoudnessNorm(integratedLoudness = -16f, truePeak = -1f, loudnessRange = 8f)
    }
}
