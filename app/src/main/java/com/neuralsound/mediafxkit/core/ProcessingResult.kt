package com.neuralsound.mediafxkit.core

/**
 * Sealed class representing the result of an FFmpeg processing operation.
 */
sealed class ProcessingResult {
    
    /**
     * Successful processing result.
     * @param outputPath Path to the output file
     * @param durationMs Processing duration in milliseconds
     * @param logs FFmpeg execution logs
     */
    data class Success(
        val outputPath: String,
        val durationMs: Long,
        val logs: String = ""
    ) : ProcessingResult()
    
    /**
     * Failed processing result.
     * @param errorCode FFmpeg return code
     * @param errorMessage Human-readable error message
     * @param logs FFmpeg execution logs
     */
    data class Failure(
        val errorCode: Int,
        val errorMessage: String,
        val logs: String = ""
    ) : ProcessingResult()
    
    /**
     * Processing was cancelled.
     * @param reason Cancellation reason
     */
    data class Cancelled(
        val reason: String = "Operation cancelled by user"
    ) : ProcessingResult()
    
    /**
     * Check if the result is successful.
     */
    val isSuccess: Boolean
        get() = this is Success
    
    /**
     * Check if the result is a failure.
     */
    val isFailure: Boolean
        get() = this is Failure
    
    /**
     * Get the output path if successful, null otherwise.
     */
    fun getOutputPathOrNull(): String? = (this as? Success)?.outputPath
    
    /**
     * Get the error message if failed, null otherwise.
     */
    fun getErrorMessageOrNull(): String? = (this as? Failure)?.errorMessage
    
    /**
     * Execute action if successful.
     */
    inline fun onSuccess(action: (Success) -> Unit): ProcessingResult {
        if (this is Success) action(this)
        return this
    }
    
    /**
     * Execute action if failed.
     */
    inline fun onFailure(action: (Failure) -> Unit): ProcessingResult {
        if (this is Failure) action(this)
        return this
    }
    
    /**
     * Execute action if cancelled.
     */
    inline fun onCancelled(action: (Cancelled) -> Unit): ProcessingResult {
        if (this is Cancelled) action(this)
        return this
    }
}
