package com.neuralsound.mediafxkit.effects

/**
 * High-pass filter effect using FFmpeg's highpass filter.
 * Removes frequencies below the specified cutoff to eliminate low-frequency rumble.
 *
 * @param cutoffHz Cutoff frequency in Hz (frequencies below this are attenuated)
 */
data class HighPass(
    val cutoffHz: Int = 80
) : AudioEffect() {

    init {
        require(cutoffHz > 0) { "Cutoff frequency must be positive" }
    }

    override fun toFilterString(): String = "highpass=f=$cutoffHz"

    override fun description(): String = "High-Pass Filter (${cutoffHz}Hz)"

    companion object {
        /** Remove sub-bass rumble (common vocal recording cleanup). */
        fun rumbleRemoval() = HighPass(cutoffHz = 80)

        /** Mild high-pass for vocals. */
        fun vocal() = HighPass(cutoffHz = 100)

        /** Aggressive high-pass for cleaning. */
        fun aggressive() = HighPass(cutoffHz = 150)
    }
}
