package net.pythonsden.ao3_.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import net.pythonsden.ao3_.data.SettingsRepository
import net.pythonsden.ao3_.data.repository.EpubRepository
import net.pythonsden.ao3_.data.repository.FileRepository

class ViewModelFactory(
    private val settingsRepository: SettingsRepository,
    private val fileRepository: FileRepository,
    private val epubRepository: EpubRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(MainViewModel::class.java) -> {
                MainViewModel(settingsRepository) as T
            }
            modelClass.isAssignableFrom(BrowserViewModel::class.java) -> {
                BrowserViewModel(settingsRepository) as T
            }
            modelClass.isAssignableFrom(DownloadsViewModel::class.java) -> {
                DownloadsViewModel(fileRepository, epubRepository) as T
            }
            modelClass.isAssignableFrom(ReaderViewModel::class.java) -> {
                ReaderViewModel(epubRepository, settingsRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
