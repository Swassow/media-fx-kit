package com.neuralsound.mediafxkit.fx

/**
 * Handle for an asynchronous FX session. Can be passed to cancel() to stop processing.
 *
 * @param sessionId Unique identifier for the FFmpeg session
 */
data class FxSession(
    val sessionId: Long
)
