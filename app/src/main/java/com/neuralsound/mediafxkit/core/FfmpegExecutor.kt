package com.neuralsound.mediafxkit.core

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Executor for FFmpeg commands with synchronous and asynchronous support.
 */
object FfmpegExecutor {
    
    private var currentSession: FFmpegSession? = null
    
    /**
     * Execute an FFmpeg command synchronously.
     * @param command The FFmpeg command arguments (without 'ffmpeg' prefix)
     * @param outputPath Expected output file path
     * @return ProcessingResult indicating success or failure
     */
    fun executeSync(command: String, outputPath: String): ProcessingResult {
        val startTime = System.currentTimeMillis()
        
        val session = FFmpegKit.execute(command)
        currentSession = session
        
        val duration = System.currentTimeMillis() - startTime
        val logs = session.allLogsAsString ?: ""
        
        return when {
            ReturnCode.isSuccess(session.returnCode) -> {
                ProcessingResult.Success(
                    outputPath = outputPath,
                    durationMs = duration,
                    logs = logs
                )
            }
            ReturnCode.isCancel(session.returnCode) -> {
                ProcessingResult.Cancelled()
            }
            else -> {
                ProcessingResult.Failure(
                    errorCode = session.returnCode?.value ?: -1,
                    errorMessage = session.failStackTrace ?: "Unknown error",
                    logs = logs
                )
            }
        }
    }
    
    /**
     * Execute an FFmpeg command asynchronously using coroutines.
     * @param command The FFmpeg command arguments (without 'ffmpeg' prefix)
     * @param outputPath Expected output file path
     * @param onProgress Optional callback for progress updates (0.0 to 1.0)
     * @return ProcessingResult indicating success or failure
     */
    suspend fun executeAsync(
        command: String,
        outputPath: String,
        onProgress: ((Float) -> Unit)? = null
    ): ProcessingResult = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            val startTime = System.currentTimeMillis()
            
            val session = FFmpegKit.executeAsync(
                command,
                { session ->
                    val duration = System.currentTimeMillis() - startTime
                    val logs = session.allLogsAsString ?: ""
                    
                    val result = when {
                        ReturnCode.isSuccess(session.returnCode) -> {
                            ProcessingResult.Success(
                                outputPath = outputPath,
                                durationMs = duration,
                                logs = logs
                            )
                        }
                        ReturnCode.isCancel(session.returnCode) -> {
                            ProcessingResult.Cancelled()
                        }
                        else -> {
                            ProcessingResult.Failure(
                                errorCode = session.returnCode?.value ?: -1,
                                errorMessage = session.failStackTrace ?: "Unknown error",
                                logs = logs
                            )
                        }
                    }
                    
                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                },
                { log ->
                    // Log callback - can be used for debugging
                },
                { statistics ->
                    // Statistics callback for progress
                    onProgress?.let { callback ->
                        val time = statistics.time
                        if (time > 0) {
                            // Progress is estimated based on time
                            // For accurate progress, duration must be known beforehand
                            callback(time.toFloat() / 1000f)
                        }
                    }
                }
            )
            
            currentSession = session
            
            continuation.invokeOnCancellation {
                FFmpegKit.cancel(session.sessionId)
            }
        }
    }
    
    /**
     * Cancel the currently running FFmpeg session.
     */
    fun cancel() {
        currentSession?.let { session ->
            FFmpegKit.cancel(session.sessionId)
        }
    }
    
    /**
     * Cancel a specific FFmpeg session by its session ID.
     */
    fun cancel(sessionId: Long) {
        FFmpegKit.cancel(sessionId)
    }
    
    /**
     * Cancel all running FFmpeg sessions.
     */
    fun cancelAll() {
        FFmpegKit.cancel()
    }
}
