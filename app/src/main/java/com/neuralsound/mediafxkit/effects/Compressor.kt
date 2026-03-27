package com.neuralsound.mediafxkit.effects

import com.neuralsound.mediafxkit.util.FilterEscapeUtils

/**
 * Dynamic range compressor effect using FFmpeg's acompressor filter.
 * 
 * @param threshold Level above which compression is applied (in dB, typically -30 to 0)
 * @param ratio Compression ratio (e.g., 4 means 4:1 compression)
 * @param attack Attack time in milliseconds
 * @param release Release time in milliseconds
 * @param makeupGain Makeup gain in dB to compensate for compression
 * @param knee Knee width in dB (soft knee compression)
 */
data class Compressor(
    val threshold: Float = -20f,
    val ratio: Float = 4f,
    val attack: Float = 5f,
    val release: Float = 50f,
    val makeupGain: Float = 0f,
    val knee: Float = 2.8f
) : AudioEffect() {
    
    override fun toFilterString(): String {
        val params = buildString {
            append("threshold=${FilterEscapeUtils.formatFloat(threshold)}dB")
            append(":ratio=${FilterEscapeUtils.formatFloat(ratio)}")
            append(":attack=${FilterEscapeUtils.formatFloat(attack)}")
            append(":release=${FilterEscapeUtils.formatFloat(release)}")
            if (makeupGain != 0f) {
                append(":makeup=${FilterEscapeUtils.formatFloat(makeupGain)}dB")
            }
            append(":knee=${FilterEscapeUtils.formatFloat(knee)}dB")
        }
        return "acompressor=$params"
    }
    
    override fun description(): String = "Compressor (${ratio}:1 @ ${threshold}dB)"
    
    companion object {
        /**
         * Light compression for gentle dynamics control.
         */
        fun light() = Compressor(
            threshold = -18f,
            ratio = 2f,
            attack = 10f,
            release = 100f,
            knee = 6f
        )
        
        /**
         * Medium compression for balanced dynamics.
         */
        fun medium() = Compressor(
            threshold = -20f,
            ratio = 4f,
            attack = 5f,
            release = 50f,
            knee = 3f
        )
        
        /**
         * Heavy compression for aggressive limiting.
         */
        fun heavy() = Compressor(
            threshold = -24f,
            ratio = 8f,
            attack = 1f,
            release = 25f,
            knee = 1f
        )
        
        /**
         * Vocal compression preset.
         */
        fun vocal() = Compressor(
            threshold = -18f,
            ratio = 3f,
            attack = 8f,
            release = 80f,
            makeupGain = 3f,
            knee = 4f
        )
        
        /**
         * Limiting preset (very high ratio).
         */
        fun limiter(ceiling: Float = -1f) = Compressor(
            threshold = ceiling,
            ratio = 20f,
            attack = 0.1f,
            release = 10f,
            knee = 0f
        )
    }
}
