package com.neuralsound.mediafxkit.effects

import com.neuralsound.mediafxkit.util.FilterEscapeUtils

/**
 * Pitch shift effect using FFmpeg's asetrate and aresample filters.
 * 
 * @param semitones Number of semitones to shift (-12 to +12 typical)
 */
data class PitchShift(
    val semitones: Float
) : AudioEffect() {
    
    /**
     * Calculate the sample rate multiplier for the given semitones.
     * Each semitone is a factor of 2^(1/12).
     */
    private val multiplier: Double
        get() = Math.pow(2.0, semitones.toDouble() / 12.0)
    
    override fun toFilterString(): String {
        // asetrate changes pitch but also speed
        // We follow with atempo to compensate and keep original speed
        // For pitch down: asetrate lowers rate (slower+lower), atempo speeds up
        // For pitch up: asetrate raises rate (faster+higher), atempo slows down
        
        val mult = FilterEscapeUtils.formatDouble(multiplier, 6)
        val tempoCompensation = FilterEscapeUtils.formatDouble(1.0 / multiplier, 6)
        
        return "asetrate=44100*$mult,aresample=44100,atempo=$tempoCompensation"
    }
    
    override fun description(): String {
        val direction = if (semitones >= 0) "+" else ""
        return "Pitch Shift ($direction${semitones} semitones)"
    }
    
    companion object {
        /**
         * Shift up by one octave.
         */
        fun octaveUp() = PitchShift(12f)
        
        /**
         * Shift down by one octave.
         */
        fun octaveDown() = PitchShift(-12f)
        
        /**
         * Shift up by a perfect fifth (7 semitones).
         */
        fun fifthUp() = PitchShift(7f)
        
        /**
         * Shift down by a perfect fifth.
         */
        fun fifthDown() = PitchShift(-7f)
        
        /**
         * Create pitch shift from cents (100 cents = 1 semitone).
         */
        fun fromCents(cents: Int) = PitchShift(cents / 100f)
    }
}
