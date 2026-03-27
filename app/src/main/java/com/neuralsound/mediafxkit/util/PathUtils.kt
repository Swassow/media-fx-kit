package com.neuralsound.mediafxkit.util

import java.io.File

/**
 * Utility functions for file path handling.
 */
object PathUtils {
    
    /**
     * Get the file extension from a path.
     */
    fun getExtension(path: String): String {
        return File(path).extension.lowercase()
    }
    
    /**
     * Get the filename without extension.
     */
    fun getNameWithoutExtension(path: String): String {
        return File(path).nameWithoutExtension
    }
    
    /**
     * Get the parent directory of a path.
     */
    fun getParentDirectory(path: String): String? {
        return File(path).parent
    }
    
    /**
     * Generate an output path based on input path with a suffix.
     * Example: input.mp3 with suffix "_processed" -> input_processed.mp3
     */
    fun generateOutputPath(inputPath: String, suffix: String): String {
        val file = File(inputPath)
        val name = file.nameWithoutExtension
        val ext = file.extension
        val parent = file.parent ?: ""
        
        return if (parent.isNotEmpty()) {
            "$parent/${name}${suffix}.$ext"
        } else {
            "${name}${suffix}.$ext"
        }
    }
    
    /**
     * Generate an output path with a different extension.
     */
    fun changeExtension(path: String, newExtension: String): String {
        val file = File(path)
        val name = file.nameWithoutExtension
        val parent = file.parent ?: ""
        val ext = newExtension.removePrefix(".")
        
        return if (parent.isNotEmpty()) {
            "$parent/$name.$ext"
        } else {
            "$name.$ext"
        }
    }
    
    /**
     * Generate a unique temporary file path.
     */
    fun generateTempPath(basePath: String, extension: String = "tmp"): String {
        val timestamp = System.currentTimeMillis()
        val random = (Math.random() * 10000).toInt()
        val ext = extension.removePrefix(".")
        
        return "$basePath/temp_${timestamp}_$random.$ext"
    }
    
    /**
     * Check if a file exists at the given path.
     */
    fun exists(path: String): Boolean {
        return File(path).exists()
    }
    
    /**
     * Delete a file at the given path.
     * @return true if deleted successfully, false otherwise
     */
    fun delete(path: String): Boolean {
        return try {
            File(path).delete()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get file size in bytes.
     * @return file size or -1 if file doesn't exist
     */
    fun getFileSize(path: String): Long {
        val file = File(path)
        return if (file.exists()) file.length() else -1L
    }
    
    /**
     * Ensure parent directory exists.
     * @return true if directory exists or was created
     */
    fun ensureParentDirectory(path: String): Boolean {
        val parent = File(path).parentFile
        return parent?.exists() == true || parent?.mkdirs() == true
    }
}
