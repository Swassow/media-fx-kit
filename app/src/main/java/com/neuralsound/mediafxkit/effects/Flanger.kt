package com.neuralsound.mediafxkit.effects

import com.neuralsound.mediafxkit.util.FilterEscapeUtils

/**
 * Flanger effect using FFmpeg's flanger filter.
 * Creates a sweeping, jet-like modulation effect.
 *
 * @param delayMs Base delay in milliseconds (0–30)
 * @param depth Modulation depth in milliseconds (0–10)
 * @param speed Modulation speed in Hz (0.1–10)
 * @param width Percentage of delayed signal mixed with original (0–100)
 * @param shape LFO waveform shape ("sinusoidal" or "triangular")
 */
data class Flanger(
    val delayMs: Float = 1f,
    val depth: Float = 2f,
    val speed: Float = 10f,
    val width: Float = 80f,
    val shape: String = "sinusoidal"
) : AudioEffect() {

    init {
        require(shape in listOf("sinusoidal", "triangular")) {
            "Shape must be 'sinusoidal' or 'triangular'"
        }
    }

    override fun toFilterString(): String {
        return "flanger=delay=${FilterEscapeUtils.formatFloat(delayMs)}" +
                ":depth=${FilterEscapeUtils.formatFloat(depth)}" +
                ":speed=${FilterEscapeUtils.formatFloat(speed)}" +
                ":width=${FilterEscapeUtils.formatFloat(width)}" +
                ":shape=$shape"
    }

    override fun description(): String = "Flanger (speed=${speed}Hz)"

    companion object {
        /** Default flanger effect. */
        fun default() = Flanger()

        /** Slow, subtle flanger. */
        fun subtle() = Flanger(delayMs = 0.5f, depth = 1f, speed = 0.5f, width = 50f)

        /** Intense jet-engine flanger. */
        fun intense() = Flanger(delayMs = 3f, depth = 5f, speed = 5f, width = 90f)
    }
}
