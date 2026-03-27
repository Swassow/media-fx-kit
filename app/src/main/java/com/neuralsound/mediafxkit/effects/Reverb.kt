package com.neuralsound.mediafxkit.effects

import com.neuralsound.mediafxkit.util.FilterEscapeUtils

/**
 * Reverb/Echo effect using FFmpeg's aecho filter.
 * 
 * @param inGain Input gain (0.0 to 1.0)
 * @param outGain Output gain (0.0 to 1.0)
 * @param delays Delay times in milliseconds (comma-separated for multiple echoes)
 * @param decays Decay factors for each delay (0.0 to 1.0, comma-separated)
 */
data class Reverb(
    val inGain: Float = 0.8f,
    val outGain: Float = 0.9f,
    val delays: String = "60|100",
    val decays: String = "0.3|0.25"
) : AudioEffect() {
    
    override fun toFilterString(): String {
        val ig = FilterEscapeUtils.formatFloat(inGain)
        val og = FilterEscapeUtils.formatFloat(outGain)
        return "aecho=$ig:$og:$delays:$decays"
    }
    
    override fun description(): String = "Reverb (delay: $delays ms)"
    
    companion object {
        /**
         * Light room reverb preset.
         */
        fun lightRoom() = Reverb(
            inGain = 0.8f,
            outGain = 0.88f,
            delays = "60",
            decays = "0.3"
        )
        
        /**
         * Medium hall reverb preset.
         */
        fun mediumHall() = Reverb(
            inGain = 0.8f,
            outGain = 0.9f,
            delays = "100|200",
            decays = "0.4|0.3"
        )
        
        /**
         * Large hall reverb preset.
         */
        fun largeHall() = Reverb(
            inGain = 0.8f,
            outGain = 0.85f,
            delays = "150|300|450",
            decays = "0.5|0.4|0.3"
        )
        
        /**
         * Cathedral reverb preset.
         */
        fun cathedral() = Reverb(
            inGain = 0.6f,
            outGain = 0.8f,
            delays = "200|400|600|800",
            decays = "0.6|0.5|0.4|0.3"
        )
    }
}
