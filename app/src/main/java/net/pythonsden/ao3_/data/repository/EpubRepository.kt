package net.pythonsden.ao3_.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.pythonsden.ao3_.data.model.EpubMetadata
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.domain.Resource
import nl.siegmann.epublib.epub.EpubReader
import java.io.File
import java.io.FileInputStream
import java.nio.charset.StandardCharsets

class EpubRepository {

    private val seriesRegex = Regex("Series:\\s*Part\\s*(\\d+)\\s*of\\s*([^\\n\\r<]+)", RegexOption.IGNORE_CASE)

    suspend fun parseMetadata(file: File): EpubMetadata = withContext(Dispatchers.IO) {
        FileInputStream(file).use { fis ->
            val book = EpubReader().readEpub(fis)
            val title = book.metadata.titles.firstOrNull() ?: file.nameWithoutExtension
            val author = book.metadata.authors.firstOrNull()?.let { "${it.firstname} ${it.lastname}".trim() } ?: "Unknown Author"
            
            var seriesName: String? = null
            var seriesPart: Int? = null
            
            // Search in metadata first if available (epublib might not expose custom ones easily)
            // Fallback to content scanning as in original code, but improved
            for (resource in book.contents) {
                val text = String(resource.data, StandardCharsets.UTF_8)
                val cleanText = text.replace(Regex("<[^>]*>"), " ")
                val match = seriesRegex.find(cleanText)
                if (match != null) {
                    seriesPart = match.groupValues[1].toIntOrNull()
                    seriesName = match.groupValues[2].trim()
                    break
                }
            }

            EpubMetadata(
                title = title,
                author = author,
                seriesName = seriesName,
                seriesPart = seriesPart,
                chapterCount = book.spine.spineReferences.size
            )
        }
    }

    suspend fun getBookContent(file: File): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        try {
            FileInputStream(file).use { fis ->
                val book = EpubReader().readEpub(fis)
                val title = book.title ?: file.name
                val sb = StringBuilder()
                
                sb.append("<html><head><style>")
                sb.append("body { font-family: sans-serif; padding: 16px; line-height: 1.6; color: #E0E0E0; background-color: #121212; }")
                sb.append("hr { border: 0; border-top: 1px solid #333; margin: 20px 0; }")
                sb.append("a { color: #BB86FC; }")
                sb.append("img { max-width: 100%; height: auto; }")
                sb.append("</style></head><body>")
                
                book.contents.forEach { resource: Resource ->
                    val content = String(resource.data, StandardCharsets.UTF_8)
                    val bodyContent = if (content.contains("<body", ignoreCase = true)) {
                        content.substringAfter("<body").substringAfter(">").substringBeforeLast("</body>")
                    } else {
                        content
                    }
                    
                    if (bodyContent.isNotBlank()) {
                        sb.append(bodyContent)
                        sb.append("<hr/>")
                    }
                }
                sb.append("</body></html>")
                Result.success(Pair(title, sb.toString()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
