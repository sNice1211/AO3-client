package net.pythonsden.ao3_.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import net.pythonsden.ao3_.data.repository.EpubRepository
import net.pythonsden.ao3_.data.repository.FileRepository
import java.io.File

enum class SortOrder { NAME, DATE, SIZE }

class DownloadsViewModel(
    private val fileRepository: FileRepository,
    private val epubRepository: EpubRepository
) : ViewModel() {

    private val _currentDir = MutableStateFlow(fileRepository.getDownloadsDir())
    val currentDir: StateFlow<File> = _currentDir.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.NAME)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _isOrganizing = MutableStateFlow(false)
    val isOrganizing: StateFlow<Boolean> = _isOrganizing.asStateFlow()

    private val _items = MutableStateFlow<List<File>>(emptyList())
    val items: StateFlow<List<File>> = _items.asStateFlow()

    val baseDir: File = fileRepository.getDownloadsDir()

    init {
        viewModelScope.launch {
            combine(_currentDir, _searchQuery, _sortOrder) { dir, query, sort ->
                Triple(dir, query, sort)
            }.collect { (dir, query, sort) ->
                refreshFiles(dir, query, sort)
            }
        }
    }

    private suspend fun refreshFiles(dir: File, query: String, sort: SortOrder) {
        val allItems = fileRepository.listFiles(dir)
        val filtered = if (query.isBlank()) {
            allItems
        } else {
            allItems.filter { it.name.contains(query, ignoreCase = true) }
        }

        _items.value = filtered.sortedWith { f1, f2 ->
            if (f1.isDirectory && !f2.isDirectory) -1
            else if (!f1.isDirectory && f2.isDirectory) 1
            else {
                when (sort) {
                    SortOrder.NAME -> f1.name.lowercase().compareTo(f2.name.lowercase())
                    SortOrder.DATE -> f2.lastModified().compareTo(f1.lastModified())
                    SortOrder.SIZE -> f2.length().compareTo(f1.length())
                }
            }
        }
    }

    fun navigateTo(dir: File) {
        _currentDir.value = dir
    }

    fun navigateUp(): Boolean {
        val current = _currentDir.value
        return if (current != baseDir) {
            _currentDir.value = current.parentFile ?: baseDir
            true
        } else {
            false
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun deleteFile(file: File, onComplete: () -> Unit) {
        viewModelScope.launch {
            fileRepository.deleteFile(file)
            refreshFiles(_currentDir.value, _searchQuery.value, _sortOrder.value)
            onComplete()
        }
    }

    fun moveFile(file: File, targetDir: File, onComplete: () -> Unit) {
        viewModelScope.launch {
            fileRepository.moveFile(file, File(targetDir, file.name))
            refreshFiles(_currentDir.value, _searchQuery.value, _sortOrder.value)
            onComplete()
        }
    }

    fun organizeFiles(onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            _isOrganizing.value = true
            val allFiles = baseDir.walkTopDown().filter { it.isFile && it.extension.lowercase() == "epub" }.toList()
            var movedCount = 0

            allFiles.forEach { file ->
                try {
                    val metadata = epubRepository.parseMetadata(file)
                    val author = metadata.author.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                    
                    val targetDir = if (!metadata.seriesName.isNullOrBlank()) {
                        val seriesName = metadata.seriesName.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                        File(baseDir, "$author/$seriesName")
                    } else {
                        File(baseDir, author)
                    }

                    if (fileRepository.safeMoveFile(file, targetDir)) {
                        movedCount++
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            _isOrganizing.value = false
            refreshFiles(_currentDir.value, _searchQuery.value, _sortOrder.value)
            onComplete(movedCount)
        }
    }
}
