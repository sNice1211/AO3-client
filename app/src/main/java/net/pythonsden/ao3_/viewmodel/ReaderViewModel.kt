package net.pythonsden.ao3_.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.pythonsden.ao3_.data.SettingsRepository
import net.pythonsden.ao3_.data.repository.EpubRepository
import java.io.File

sealed class ReaderUiState {
    object Loading : ReaderUiState()
    data class Success(val title: String, val content: String, val initialScroll: Int) : ReaderUiState()
    data class Error(val message: String) : ReaderUiState()
}

class ReaderViewModel(
    private val epubRepository: EpubRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading)
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    fun loadEpub(file: File) {
        viewModelScope.launch {
            _uiState.value = ReaderUiState.Loading
            val initialScroll = settingsRepository.getReaderScrollFlow(file.absolutePath).first()
            epubRepository.getBookContent(file).onSuccess { (title, content) ->
                _uiState.value = ReaderUiState.Success(title, content, initialScroll)
                settingsRepository.updateLastEpubPath(file.absolutePath)
            }.onFailure {
                _uiState.value = ReaderUiState.Error(it.message ?: "Unknown error")
            }
        }
    }

    fun saveScrollPosition(filePath: String, scrollY: Int) {
        viewModelScope.launch {
            if (scrollY > 0) {
                settingsRepository.updateReaderScroll(filePath, scrollY)
            }
        }
    }
}
