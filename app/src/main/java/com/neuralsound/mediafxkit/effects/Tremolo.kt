package com.neuralsound.mediafxkit.effects

import com.neuralsound.mediafxkit.util.FilterEscapeUtils

/**
 * Tremolo effect using FFmpeg's tremolo filter.
 * Applies amplitude modulation to create a pulsating volume effect.
 *
 * @param frequency Modulation rate in Hz (0.1–20000)
 * @param depth Modulation depth (0.0–1.0, where 1.0 is full modulation)
 */
data class Tremolo(
    val frequency: Double = 5.0,
    val depth: Double = 0.5
) : AudioEffect() {

    init {
        require(frequency > 0) { "Frequency must be positive" }
        require(depth in 0.0..1.0) { "Depth must be between 0.0 and 1.0" }
    }

    override fun toFilterString(): String {
        val f = FilterEscapeUtils.formatDouble(frequency)
        val d = FilterEscapeUtils.formatDouble(depth)
        return "tremolo=f=$f:d=$d"
    }

    override fun description(): String = "Tremolo (${frequency}Hz, depth=${depth})"

    companion object {
        /** Slow, subtle tremolo. */
        fun slow() = Tremolo(frequency = 3.0, depth = 0.3)

        /** Medium tremolo for standard effect. */
        fun medium() = Tremolo(frequency = 5.0, depth = 0.5)

        /** Fast, intense tremolo. */
        fun fast() = Tremolo(frequency = 10.0, depth = 0.7)

        /** Vocoder-style tremolo. */
        fun vocoder() = Tremolo(frequency = 8.0, depth = 0.6)
    }
}
