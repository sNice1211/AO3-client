# AO3 Client (v1.0)

A feature-rich Android client for Archive of Our Own (AO3), designed for a seamless browsing, downloading, and reading experience.

## Features

- **Integrated AO3 Browser**: Browse the Archive with a built-in WebView that supports navigation, cookie management, and session persistence.
- **Direct Downloads**: Intercepts EPUB downloads from AO3 and saves them directly to your local library.
- **Smart Library Organization**: 
    - Automatically organizes your downloaded works into folders by Author.
    - Recognizes Series information from EPUB metadata and organizes works into subfolders for each series.
    - Cleans and formats file/folder names for better compatibility.
- **Built-in EPUB Reader**:
    - Custom reader optimized for fanfiction.
    - Dark mode by default for comfortable reading.
    - **Persistent Progress**: Remembers your scroll position for every work using a deterministic layout-based restoration.
- **Offline Mode**: A dedicated toggle to browse your downloaded library without an internet connection.
- **Library Management**:
    - Search through your downloaded works.
    - Sort by Name, Date, or File Size.
    - Move, delete, and manually manage your files with safety confirmations.
- **State Persistence**: The app remembers where you left off, including your last visited URL and the last book you were reading, powered by **Jetpack DataStore**.

## Tech Stack & Architecture

This project follows **Clean Architecture** principles and modern Android development practices:

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose)
- **State Management**: [ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel) with [StateFlow](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-state-flow/)
- **Persistence**: [Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore) (Preferences)
- **Concurrency**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) for non-blocking I/O
- **EPUB Engine**: [epublib](https://github.com/psiegman/epublib)
- **Design System**: Material 3

## Installation

### Method 1: Direct APK (Recommended for users)
1. Download the latest `app-release.apk` (or `app-debug.apk` for testing).
2. Transfer the file to your Android device.
3. Open the file on your device to install.
   - *Note: You may need to enable "Install from Unknown Sources" in your device settings.*

### Method 2: Build from Source (For developers)
1. Clone the repository:
   ```bash
   git clone https://github.com/sNice1211/AO3-client.git
   ```
2. Open the project in Android Studio.
3. Build and run the app on your device or emulator.

## Usage

1. **Browsing**: Use the 'Browse' tab to find your favorite fics on AO3.
2. **Downloading**: Tap the 'Download' button on any AO3 work page and select 'EPUB'. The app will handle the rest.
3. **Organizing**: Go to the 'Downloads' tab and tap the 'Magic Wand' (Organize) icon to automatically sort your files by author and series.
4. **Reading**: Simply tap on any downloaded work in your library to open the internal reader.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Acknowledgements

This project was developed and refactored with the assistance of **AI technology**, which helped in modernizing the architecture, improving performance, and implementing robust state management.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

---
*Disclaimer: This is an unofficial client and is not affiliated with or endorsed by the Organization for Transformative Works (OTW) or Archive of Our Own.*
