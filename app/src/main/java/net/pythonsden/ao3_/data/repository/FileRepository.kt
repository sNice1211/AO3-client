package net.pythonsden.ao3_.data.repository

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class FileRepository(private val context: Context) {

    fun getDownloadsDir(): File {
        return context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
    }

    suspend fun listFiles(directory: File): List<File> = withContext(Dispatchers.IO) {
        directory.listFiles()?.toList() ?: emptyList()
    }

    suspend fun deleteFile(file: File): Boolean = withContext(Dispatchers.IO) {
        if (file.isDirectory) {
            file.deleteRecursively()
        } else {
            file.delete()
        }
    }

    suspend fun moveFile(source: File, target: File): Boolean = withContext(Dispatchers.IO) {
        try {
            if (target.exists()) {
                // Simple collision handling: append timestamp if it's a file
                if (target.isFile) {
                    val newTarget = File(target.parent, "${target.nameWithoutExtension}_${System.currentTimeMillis()}.${target.extension}")
                    source.renameTo(newTarget)
                } else {
                    false
                }
            } else {
                val parent = target.parentFile
                if (parent != null && !parent.exists()) {
                    parent.mkdirs()
                }
                source.renameTo(target)
            }
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun safeMoveFile(source: File, targetDir: File): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }
            var targetFile = File(targetDir, source.name)
            if (targetFile.exists() && targetFile.absolutePath != source.absolutePath) {
                val name = source.nameWithoutExtension
                val ext = source.extension
                var counter = 1
                while (targetFile.exists()) {
                    targetFile = File(targetDir, "${name}_$counter.$ext")
                    counter++
                }
            }
            
            if (source.absolutePath == targetFile.absolutePath) return@withContext true
            
            val success = source.renameTo(targetFile)
            if (!success) {
                // Fallback to copy and delete if rename fails (e.g. across partitions, though unlikely here)
                try {
                    source.inputStream().use { input ->
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    source.delete()
                    true
                } catch (e: IOException) {
                    false
                }
            } else {
                true
            }
        } catch (e: Exception) {
            false
        }
    }
}
