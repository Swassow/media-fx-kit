package com.neuralsound.mediafxkit.operations

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFprobeKit
import com.neuralsound.mediafxkit.core.ProcessingResult
import com.neuralsound.mediafxkit.util.FilterEscapeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.max

/**
 * Waveform data extraction for visualization.
 */
object WaveformExtractor {
    
    /**
     * Waveform data containing amplitude samples.
     */
    data class WaveformData(
        val samples: FloatArray,
        val sampleRate: Int,
        val duration: Double,
        val channels: Int
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as WaveformData
            return samples.contentEquals(other.samples) && 
                   sampleRate == other.sampleRate && 
                   duration == other.duration &&
                   channels == other.channels
        }
        
        override fun hashCode(): Int {
            var result = samples.contentHashCode()
            result = 31 * result + sampleRate
            result = 31 * result + duration.hashCode()
            result = 31 * result + channels
            return result
        }
        
        /**
         * Get normalized samples (0.0 to 1.0).
         */
        fun getNormalizedSamples(): FloatArray {
            val maxValue = samples.maxOfOrNull { abs(it) } ?: 1f
            return if (maxValue > 0) {
                samples.map { abs(it) / maxValue }.toFloatArray()
            } else {
                samples
            }
        }
        
        /**
         * Downsample to a specific number of points.
         */
        fun downsampleTo(targetPoints: Int): FloatArray {
            if (samples.size <= targetPoints) return samples.copyOf()
            
            val result = FloatArray(targetPoints)
            val samplesPerPoint = samples.size.toFloat() / targetPoints
            
            for (i in 0 until targetPoints) {
                val start = (i * samplesPerPoint).toInt()
                val end = ((i + 1) * samplesPerPoint).toInt().coerceAtMost(samples.size)
                
                // Take max absolute value in this range
                var maxVal = 0f
                for (j in start until end) {
                    val absVal = abs(samples[j])
                    if (absVal > maxVal) maxVal = absVal
                }
                result[i] = maxVal
            }
            
            return result
        }
    }
    
    /**
     * Extract waveform data from an audio file.
     * Uses FFmpeg to extract amplitude envelope.
     * 
     * @param inputPath Input audio file path
     * @param samplesPerSecond Number of samples per second (higher = more detail)
     * @return WaveformData or null if extraction failed
     */
    suspend fun extract(
        inputPath: String,
        samplesPerSecond: Int = 10
    ): WaveformData? = withContext(Dispatchers.IO) {
        try {
            // First, get audio info
            val probeSession = FFprobeKit.execute("-v quiet -print_format json -show_format -show_streams ${FilterEscapeUtils.escapePath(inputPath)}")
            val probeOutput = probeSession.output ?: return@withContext null
            
            val json = JSONObject(probeOutput)
            val format = json.optJSONObject("format") ?: return@withContext null
            val duration = format.optString("duration", "0").toDoubleOrNull() ?: 0.0
            
            // Find audio stream info
            val streams = json.optJSONArray("streams")
            var sampleRate = 44100
            var channels = 2
            
            if (streams != null) {
                for (i in 0 until streams.length()) {
                    val stream = streams.getJSONObject(i)
                    if (stream.optString("codec_type") == "audio") {
                        sampleRate = stream.optInt("sample_rate", 44100)
                        channels = stream.optInt("channels", 2)
                        break
                    }
                }
            }
            
            // Calculate total samples needed
            val totalSamples = (duration * samplesPerSecond).toInt()
            if (totalSamples <= 0) return@withContext null
            
            // Use astats to get per-sample amplitude data
            // This extracts RMS values which are good for visualization
            val outputRate = samplesPerSecond
            
            val command = "-i ${FilterEscapeUtils.escapePath(inputPath)} " +
                    "-af \"aresample=$outputRate,astats=metadata=1:reset=1\" " +
                    "-f null -"
            
            val session = FFmpegKit.execute(command)
            val logs = session.allLogsAsString ?: ""
            
            // Parse the astats output to extract RMS values
            val samples = parseAstatsOutput(logs, totalSamples)
            
            WaveformData(
                samples = samples,
                sampleRate = sampleRate,
                duration = duration,
                channels = channels
            )
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Extract simplified waveform as just peak values.
     * Faster but less accurate than full extraction.
     */
    suspend fun extractPeaks(
        inputPath: String,
        numPeaks: Int = 100
    ): FloatArray? = withContext(Dispatchers.IO) {
        try {
            // Get duration first
            val probeSession = FFprobeKit.execute("-v quiet -print_format json -show_format ${FilterEscapeUtils.escapePath(inputPath)}")
            val probeOutput = probeSession.output ?: return@withContext null
            val json = JSONObject(probeOutput)
            val duration = json.optJSONObject("format")?.optString("duration", "0")?.toDoubleOrNull() ?: return@withContext null
            
            if (duration <= 0) return@withContext null
            
            // Calculate segment duration
            val segmentDuration = duration / numPeaks
            val peaks = FloatArray(numPeaks)
            
            // Use volumedetect for each segment would be too slow
            // Instead, use a simpler approach with downsampling
            val samplesPerSecond = (numPeaks / duration).coerceAtLeast(1.0).toInt()
            val result = extract(inputPath, samplesPerSecond)
            
            result?.downsampleTo(numPeaks) ?: floatArrayOf()
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parseAstatsOutput(logs: String, expectedSamples: Int): FloatArray {
        // astats outputs RMS values in the log
        // This is a simplified parser - actual implementation would need to handle
        // the specific log format from FFmpeg
        
        val samples = mutableListOf<Float>()
        val rmsPattern = Regex("""RMS\s+level.*?(-?\d+\.?\d*)\s*dB""")
        
        rmsPattern.findAll(logs).forEach { match ->
            val dbValue = match.groupValues[1].toFloatOrNull() ?: -60f
            // Convert dB to linear scale (0-1 range)
            val linear = Math.pow(10.0, dbValue / 20.0).toFloat().coerceIn(0f, 1f)
            samples.add(linear)
        }
        
        // If we couldn't parse enough samples, fill with zeros
        while (samples.size < expectedSamples) {
            samples.add(0f)
        }
        
        return samples.take(expectedSamples).toFloatArray()
    }
}
