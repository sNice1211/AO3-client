package net.pythonsden.ao3_

import android.app.Application
import net.pythonsden.ao3_.data.SettingsRepository
import net.pythonsden.ao3_.data.repository.EpubRepository
import net.pythonsden.ao3_.data.repository.FileRepository

class AO3Application : Application() {
    lateinit var settingsRepository: SettingsRepository
    lateinit var fileRepository: FileRepository
    lateinit var epubRepository: EpubRepository

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        fileRepository = FileRepository(this)
        epubRepository = EpubRepository()
    }
}
