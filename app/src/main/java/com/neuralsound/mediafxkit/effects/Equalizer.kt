package com.neuralsound.mediafxkit.effects

import com.neuralsound.mediafxkit.util.FilterEscapeUtils

/**
 * Parametric equalizer effect using FFmpeg's equalizer filter.
 * 
 * @param bands List of EQ bands to apply
 */
data class Equalizer(
    val bands: List<Band>
) : AudioEffect() {
    
    /**
     * Single EQ band configuration.
     * 
     * @param frequency Center frequency in Hz
     * @param width Bandwidth (Q factor or width in Hz based on widthType)
     * @param gain Gain in dB (-20 to +20 typical)
     * @param widthType Type of width parameter (q, h, o, s, k)
     */
    data class Band(
        val frequency: Float,
        val width: Float = 1.0f,
        val gain: Float = 0f,
        val widthType: WidthType = WidthType.Q
    ) {
        fun toFilterString(): String {
            val f = FilterEscapeUtils.formatFloat(frequency, 0)
            val w = FilterEscapeUtils.formatFloat(width)
            val g = FilterEscapeUtils.formatFloat(gain)
            return "equalizer=f=$f:width_type=${widthType.value}:w=$w:g=$g"
        }
    }
    
    /**
     * Width type for EQ bands.
     */
    enum class WidthType(val value: String) {
        Q("q"),      // Q-factor
        H("h"),      // Hz
        OCTAVE("o"), // Octave
        SLOPE("s"),  // Slope
        KILO("k")    // kHz
    }
    
    override fun toFilterString(): String {
        return bands.joinToString(",") { it.toFilterString() }
    }
    
    override fun description(): String = "Equalizer (${bands.size} bands)"
    
    companion object {
        /**
         * Create a single-band EQ.
         */
        fun band(frequency: Float, gain: Float, width: Float = 1.0f): Equalizer {
            return Equalizer(listOf(Band(frequency, width, gain)))
        }
        
        /**
         * Bass boost preset.
         */
        fun bassBoost(amount: Float = 6f): Equalizer {
            return Equalizer(listOf(
                Band(60f, 0.7f, amount),
                Band(150f, 1.0f, amount * 0.7f),
                Band(400f, 1.0f, amount * 0.3f)
            ))
        }
        
        /**
         * Treble boost preset.
         */
        fun trebleBoost(amount: Float = 5f): Equalizer {
            return Equalizer(listOf(
                Band(3000f, 1.0f, amount * 0.5f),
                Band(6000f, 1.0f, amount * 0.8f),
                Band(12000f, 0.7f, amount)
            ))
        }
        
        /**
         * Vocal enhance preset.
         */
        fun vocalEnhance(): Equalizer {
            return Equalizer(listOf(
                Band(100f, 1.0f, -3f),   // Reduce rumble
                Band(250f, 1.5f, -2f),   // Reduce muddiness
                Band(2500f, 1.0f, 3f),   // Presence
                Band(5000f, 1.0f, 2f),   // Clarity
                Band(8000f, 1.0f, 1f)    // Air
            ))
        }
        
        /**
         * 10-band graphic EQ.
         * Frequencies: 31, 62, 125, 250, 500, 1k, 2k, 4k, 8k, 16k Hz
         */
        fun tenBand(gains: List<Float>): Equalizer {
            require(gains.size == 10) { "Must provide exactly 10 gain values" }
            val frequencies = listOf(31f, 62f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f)
            return Equalizer(
                frequencies.zip(gains).map { (freq, gain) ->
                    Band(freq, 1.0f, gain, WidthType.OCTAVE)
                }
            )
        }
    }
}
