package com.neuralsound.mediafxkit.operations

import com.neuralsound.mediafxkit.core.FfmpegCommandBuilder
import com.neuralsound.mediafxkit.core.ProcessingResult
import com.neuralsound.mediafxkit.util.PathUtils

/**
 * Audio format conversion operations.
 */
object AudioConverter {
    
    /**
     * Supported audio formats.
     */
    enum class Format(val extension: String, val codec: String, val mimeType: String) {
        MP3("mp3", "libmp3lame", "audio/mpeg"),
        AAC("aac", "aac", "audio/aac"),
        M4A("m4a", "aac", "audio/mp4"),
        WAV("wav", "pcm_s16le", "audio/wav"),
        FLAC("flac", "flac", "audio/flac"),
        OGG("ogg", "libvorbis", "audio/ogg"),
        OPUS("opus", "libopus", "audio/opus")
    }
    
    /**
     * Quality presets for lossy formats.
     */
    enum class Quality(val bitrate: String) {
        LOW("96k"),
        MEDIUM("128k"),
        HIGH("192k"),
        VERY_HIGH("256k"),
        BEST("320k")
    }
    
    /**
     * Convert audio file to a different format.
     * 
     * @param inputPath Input file path
     * @param outputPath Output file path
     * @param format Target format
     * @param quality Quality preset for lossy formats
     * @return ProcessingResult
     */
    fun convertSync(
        inputPath: String,
        outputPath: String,
        format: Format,
        quality: Quality = Quality.HIGH
    ): ProcessingResult {
        val builder = FfmpegCommandBuilder.create()
            .input(inputPath)
            .audioCodec(format.codec)
        
        // Apply bitrate for lossy formats
        when (format) {
            Format.MP3, Format.AAC, Format.M4A, Format.OGG, Format.OPUS -> {
                builder.audioBitrate(quality.bitrate)
            }
            else -> { /* Lossless formats don't need bitrate */ }
        }
        
        return builder.output(outputPath).executeSync()
    }
    
    /**
     * Convert audio file asynchronously.
     */
    suspend fun convertAsync(
        inputPath: String,
        outputPath: String,
        format: Format,
        quality: Quality = Quality.HIGH,
        onProgress: ((Float) -> Unit)? = null
    ): ProcessingResult {
        val builder = FfmpegCommandBuilder.create()
            .input(inputPath)
            .audioCodec(format.codec)
        
        when (format) {
            Format.MP3, Format.AAC, Format.M4A, Format.OGG, Format.OPUS -> {
                builder.audioBitrate(quality.bitrate)
            }
            else -> { }
        }
        
        return builder.output(outputPath).executeAsync(onProgress)
    }
    
    /**
     * Convert to MP3 with default settings.
     */
    fun toMp3Sync(
        inputPath: String,
        outputPath: String = PathUtils.changeExtension(inputPath, "mp3"),
        quality: Quality = Quality.HIGH
    ): ProcessingResult {
        return convertSync(inputPath, outputPath, Format.MP3, quality)
    }
    
    /**
     * Convert to WAV (lossless).
     */
    fun toWavSync(
        inputPath: String,
        outputPath: String = PathUtils.changeExtension(inputPath, "wav"),
        sampleRate: Int = 44100,
        channels: Int = 2
    ): ProcessingResult {
        return FfmpegCommandBuilder.create()
            .input(inputPath)
            .audioCodec("pcm_s16le")
            .sampleRate(sampleRate)
            .channels(channels)
            .output(outputPath)
            .executeSync()
    }
    
    /**
     * Convert to FLAC (lossless compression).
     */
    fun toFlacSync(
        inputPath: String,
        outputPath: String = PathUtils.changeExtension(inputPath, "flac")
    ): ProcessingResult {
        return convertSync(inputPath, outputPath, Format.FLAC, Quality.HIGH)
    }
    
    /**
     * Extract audio from video file.
     */
    fun extractAudioSync(
        videoPath: String,
        outputPath: String,
        format: Format = Format.MP3,
        quality: Quality = Quality.HIGH
    ): ProcessingResult {
        return FfmpegCommandBuilder.create()
            .input(videoPath)
            .outputOption("-vn") // No video
            .audioCodec(format.codec)
            .audioBitrate(quality.bitrate)
            .output(outputPath)
            .executeSync()
    }
    
    /**
     * Resample audio to different sample rate.
     */
    fun resampleSync(
        inputPath: String,
        outputPath: String,
        sampleRate: Int
    ): ProcessingResult {
        return FfmpegCommandBuilder.create()
            .input(inputPath)
            .sampleRate(sampleRate)
            .output(outputPath)
            .executeSync()
    }
}
