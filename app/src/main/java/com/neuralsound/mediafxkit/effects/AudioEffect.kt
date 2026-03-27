package com.neuralsound.mediafxkit.effects

/**
 * Sealed class representing audio effects that can be applied using FFmpeg filters.
 */
sealed class AudioEffect {
    
    /**
     * Convert the effect to an FFmpeg filter string.
     */
    abstract fun toFilterString(): String
    
    /**
     * Get a human-readable description of the effect.
     */
    abstract fun description(): String
}
