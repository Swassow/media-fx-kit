package com.neuralsound.mediafxkit.effects

import com.neuralsound.mediafxkit.util.FilterEscapeUtils

/**
 * Echo/delay effect using FFmpeg's aecho filter.
 * Adds a single echo tap to the audio signal.
 *
 * @param inGain Input gain (0.0 to 1.0)
 * @param outGain Output gain (0.0 to 1.0)
 * @param delayMs Echo delay in milliseconds
 * @param decay Echo decay factor (0.0 to 1.0)
 */
data class Echo(
    val inGain: Float = 0.8f,
    val outGain: Float = 0.88f,
    val delayMs: Int = 60,
    val decay: Float = 0.4f
) : AudioEffect() {

    init {
        require(delayMs > 0) { "Delay must be positive" }
        require(decay in 0f..1f) { "Decay must be between 0.0 and 1.0" }
    }

    override fun toFilterString(): String {
        val ig = FilterEscapeUtils.formatFloat(inGain)
        val og = FilterEscapeUtils.formatFloat(outGain)
        val dec = FilterEscapeUtils.formatFloat(decay)
        return "aecho=$ig:$og:$delayMs:$dec"
    }

    override fun description(): String = "Echo (${delayMs}ms, decay=${decay})"

    companion object {
        /** Short slap-back echo. */
        fun slapback() = Echo(delayMs = 30, decay = 0.3f)

        /** Subtle room echo. */
        fun subtle() = Echo(delayMs = 60, decay = 0.4f)

        /** Vocoder-style tight echo. */
        fun tight() = Echo(delayMs = 20, decay = 0.3f)

        /** Longer delay echo. */
        fun long() = Echo(delayMs = 200, decay = 0.5f)
    }
}
