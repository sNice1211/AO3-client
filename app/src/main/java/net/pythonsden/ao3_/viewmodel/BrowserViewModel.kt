package net.pythonsden.ao3_.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.pythonsden.ao3_.data.SettingsRepository

class BrowserViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    val lastUrl: StateFlow<String> = settingsRepository.lastUrlFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "https://archiveofourown.org/")

    private val _canGoBack = MutableStateFlow(false)
    val canGoBack: StateFlow<Boolean> = _canGoBack.asStateFlow()

    fun updateLastUrl(url: String) {
        viewModelScope.launch {
            settingsRepository.updateLastUrl(url)
        }
    }

    fun setCanGoBack(canGoBack: Boolean) {
        _canGoBack.value = canGoBack
    }
}
