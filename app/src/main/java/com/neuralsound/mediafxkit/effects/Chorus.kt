package com.neuralsound.mediafxkit.effects

import com.neuralsound.mediafxkit.util.FilterEscapeUtils

/**
 * Chorus effect using FFmpeg's chorus filter.
 * Adds multiple delayed, pitch-modulated copies of the signal to create a thicker sound.
 *
 * @param inGain Input gain (0.0 to 1.0)
 * @param outGain Output gain (0.0 to 1.0)
 * @param delays Delay times in ms per voice
 * @param decays Decay factor per voice (0.0 to 1.0)
 * @param speeds Modulation speed per voice (Hz)
 * @param depths Modulation depth per voice (ms)
 */
data class Chorus(
    val inGain: Float = 0.5f,
    val outGain: Float = 0.9f,
    val delays: List<Int>,
    val decays: List<Float>,
    val speeds: List<Float>,
    val depths: List<Float>
) : AudioEffect() {

    init {
        val voiceCount = delays.size
        require(voiceCount > 0) { "At least one chorus voice is required" }
        require(decays.size == voiceCount && speeds.size == voiceCount && depths.size == voiceCount) {
            "delays, decays, speeds, and depths must all have the same size"
        }
    }

    override fun toFilterString(): String {
        val ig = FilterEscapeUtils.formatFloat(inGain)
        val og = FilterEscapeUtils.formatFloat(outGain)
        val d = delays.joinToString("|")
        val dec = decays.joinToString("|") { FilterEscapeUtils.formatFloat(it) }
        val spd = speeds.joinToString("|") { FilterEscapeUtils.formatFloat(it) }
        val dep = depths.joinToString("|") { FilterEscapeUtils.formatFloat(it) }
        return "chorus=$ig:$og:$d:$dec:$spd:$dep"
    }

    override fun description(): String = "Chorus (${delays.size} voice${if (delays.size > 1) "s" else ""})"

    companion object {
        /** Single-voice chorus for subtle thickening. */
        fun single(delay: Int = 55, decay: Float = 0.4f, speed: Float = 0.25f, depth: Float = 2f) =
            Chorus(
                delays = listOf(delay),
                decays = listOf(decay),
                speeds = listOf(speed),
                depths = listOf(depth)
            )

        /** Three-voice chorus for a wide, lush sound. */
        fun threeVoice() = Chorus(
            delays = listOf(40, 48, 32),
            decays = listOf(0.4f, 0.32f, 0.3f),
            speeds = listOf(0.25f, 0.4f, 0.3f),
            depths = listOf(2.0f, 2.3f, 1.3f)
        )

        /** Rich five-voice chorus. */
        fun fiveVoice() = Chorus(
            delays = listOf(30, 40, 50, 60, 70),
            decays = listOf(0.4f, 0.35f, 0.3f, 0.28f, 0.25f),
            speeds = listOf(0.2f, 0.3f, 0.25f, 0.35f, 0.15f),
            depths = listOf(1.5f, 2.0f, 2.5f, 1.8f, 2.2f)
        )
    }
}
