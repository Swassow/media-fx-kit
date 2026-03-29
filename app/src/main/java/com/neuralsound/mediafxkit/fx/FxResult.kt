package com.neuralsound.mediafxkit.fx

/**
 * Result wrapper returned by all FX API methods.
 *
 * @param success Whether the operation completed successfully
 * @param outputPath Path to the output file (non-null on success)
 * @param error Error message (non-null on failure)
 */
data class FxResult(
    val success: Boolean,
    val outputPath: String?,
    val error: String? = null
) {
    companion object {
        fun success(outputPath: String) = FxResult(success = true, outputPath = outputPath)
        fun failure(error: String) = FxResult(success = false, outputPath = null, error = error)
    }
}
