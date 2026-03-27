package com.neuralsound.mediafxkit.effects

import com.neuralsound.mediafxkit.util.FilterEscapeUtils

/**
 * Tempo change effect using FFmpeg's atempo filter.
 * Changes playback speed without affecting pitch.
 * 
 * @param factor Speed multiplier (0.5 = half speed, 2.0 = double speed)
 *               Valid range per atempo filter: 0.5 to 100.0
 *               For values outside 0.5-2.0, multiple atempo filters are chained.
 */
data class TempoChange(
    val factor: Float
) : AudioEffect() {
    
    init {
        require(factor > 0) { "Tempo factor must be positive" }
        require(factor >= 0.25f && factor <= 4f) { "Tempo factor must be between 0.25 and 4.0" }
    }
    
    override fun toFilterString(): String {
        // atempo filter only accepts values between 0.5 and 2.0
        // For values outside this range, we chain multiple atempo filters
        
        return when {
            factor >= 0.5f && factor <= 2.0f -> {
                "atempo=${FilterEscapeUtils.formatFloat(factor)}"
            }
            factor < 0.5f -> {
                // Chain multiple atempo filters for slower speeds
                // e.g., 0.25 = atempo=0.5,atempo=0.5
                val count = calculateChainCount(factor, slow = true)
                val perFilter = Math.pow(factor.toDouble(), 1.0 / count).toFloat()
                List(count) { "atempo=${FilterEscapeUtils.formatFloat(perFilter)}" }
                    .joinToString(",")
            }
            else -> {
                // Chain multiple atempo filters for faster speeds
                // e.g., 4.0 = atempo=2.0,atempo=2.0
                val count = calculateChainCount(factor, slow = false)
                val perFilter = Math.pow(factor.toDouble(), 1.0 / count).toFloat()
                List(count) { "atempo=${FilterEscapeUtils.formatFloat(perFilter)}" }
                    .joinToString(",")
            }
        }
    }
    
    private fun calculateChainCount(factor: Float, slow: Boolean): Int {
        // Calculate how many filters we need to chain
        var count = 1
        var remaining = factor.toDouble()
        val limit = if (slow) 0.5 else 2.0
        
        while (if (slow) remaining < limit else remaining > limit) {
            count++
            remaining = Math.pow(factor.toDouble(), 1.0 / count)
        }
        
        return count
    }
    
    override fun description(): String {
        val percentage = (factor * 100).toInt()
        return "Tempo Change (${percentage}%)"
    }
    
    companion object {
        /**
         * Half speed (50%).
         */
        fun halfSpeed() = TempoChange(0.5f)
        
        /**
         * Double speed (200%).
         */
        fun doubleSpeed() = TempoChange(2.0f)
        
        /**
         * Slow down by percentage (e.g., 25 for 25% slower).
         */
        fun slowDown(percent: Int): TempoChange {
            require(percent in 1..75) { "Slowdown percent must be between 1 and 75" }
            return TempoChange(1f - (percent / 100f))
        }
        
        /**
         * Speed up by percentage (e.g., 50 for 50% faster).
         */
        fun speedUp(percent: Int): TempoChange {
            require(percent in 1..300) { "Speedup percent must be between 1 and 300" }
            return TempoChange(1f + (percent / 100f))
        }
        
        /**
         * Create tempo from BPM change.
         */
        fun fromBpm(originalBpm: Float, targetBpm: Float): TempoChange {
            require(originalBpm > 0 && targetBpm > 0) { "BPM values must be positive" }
            return TempoChange(targetBpm / originalBpm)
        }
    }
}
