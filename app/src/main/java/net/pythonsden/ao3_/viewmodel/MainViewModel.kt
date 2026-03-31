package net.pythonsden.ao3_.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.pythonsden.ao3_.data.SettingsRepository

class MainViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    val offlineMode: StateFlow<Boolean> = settingsRepository.offlineModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val lastRoute: StateFlow<String?> = settingsRepository.lastRouteFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val lastEpubPath: StateFlow<String?> = settingsRepository.lastEpubPathFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun toggleOfflineMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateOfflineMode(enabled)
        }
    }

    fun updateLastRoute(route: String) {
        viewModelScope.launch {
            settingsRepository.updateLastRoute(route)
        }
    }

    fun updateLastEpubPath(path: String) {
        viewModelScope.launch {
            settingsRepository.updateLastEpubPath(path)
        }
    }
}
