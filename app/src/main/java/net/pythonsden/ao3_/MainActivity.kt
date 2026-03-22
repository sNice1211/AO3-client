package net.pythonsden.ao3_

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.domain.Resource
import nl.siegmann.epublib.epub.EpubReader
import net.pythonsden.ao3_.ui.theme.AO3clientTheme
import java.io.File
import java.io.FileInputStream
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupCookieManager()
        enableEdgeToEdge()
        setContent {
            AO3clientTheme {
                MainScreen()
            }
        }
    }

    private fun setupCookieManager() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
    }

    override fun onDestroy() {
        CookieManager.getInstance().flush()
        super.onDestroy()
    }
}

sealed class Screen(val route: String, val label: String, val icon: @Composable () -> Unit) {
    object Browser : Screen("browser", "Browse", { Icon(Icons.Default.Language, contentDescription = null) })
    object Downloads : Screen("downloads", "Downloads", { Icon(Icons.Default.Download, contentDescription = null) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ao3_prefs", Context.MODE_PRIVATE) }

    var isOfflineMode by remember {
        mutableStateOf(prefs.getBoolean("offline_mode", false))
    }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var canGoBackInBrowser by remember { mutableStateOf(false) }

    // Save state whenever it changes
    LaunchedEffect(navBackStackEntry) {
        navBackStackEntry?.destination?.route?.let { route ->
            val editor = prefs.edit()
            editor.putString("last_route", route)
            if (route == "reader/{filePath}") {
                val filePath = navBackStackEntry?.arguments?.getString("filePath")
                if (filePath != null) {
                    editor.putString("last_epub_path", filePath)
                }
            }
            editor.apply()
        }
    }

    // Restore state on launch
    var isRestored by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!isRestored) {
            val lastRoute = prefs.getString("last_route", null)
            val startRoute = if (isOfflineMode) Screen.Downloads.route else Screen.Browser.route
            
            if (lastRoute != null && lastRoute != startRoute) {
                if (lastRoute == "reader/{filePath}") {
                    val lastEpubPath = prefs.getString("last_epub_path", null)
                    if (lastEpubPath != null) {
                        val encoded = URLEncoder.encode(lastEpubPath, StandardCharsets.UTF_8.toString())
                        navController.navigate("reader/$encoded")
                    }
                } else if (!isOfflineMode || lastRoute == Screen.Downloads.route) {
                    navController.navigate(lastRoute) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
            isRestored = true
        }
    }

    Scaffold(
        topBar = {
            if (currentDestination?.route != "reader/{filePath}") {
                TopAppBar(
                    title = { Text("AO3 Client") },
                    navigationIcon = {
                        if (currentDestination?.route == Screen.Browser.route && canGoBackInBrowser) {
                            IconButton(onClick = { webViewRef?.goBack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    actions = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Offline Mode", style = MaterialTheme.typography.labelLarge)
                            Switch(
                                checked = isOfflineMode,
                                onCheckedChange = { checked ->
                                    isOfflineMode = checked
                                    prefs.edit().putBoolean("offline_mode", checked).apply()
                                    if (checked && currentDestination?.route == Screen.Browser.route) {
                                        navController.navigate(Screen.Downloads.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (currentDestination?.route != "reader/{filePath}") {
                NavigationBar {
                    val screens = if (isOfflineMode) listOf(Screen.Downloads) else listOf(Screen.Browser, Screen.Downloads)
                    screens.forEach { screen ->
                        NavigationBarItem(
                            icon = screen.icon,
                            label = { Text(screen.label) },
                            selected = currentDestination?.route == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (isOfflineMode) Screen.Downloads.route else Screen.Browser.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Screen.Browser.route) {
                if (isOfflineMode) {
                    Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Offline mode is enabled.")
                    }
                } else {
                    val lastUrl = prefs.getString("last_url", "https://archiveofourown.org/") ?: "https://archiveofourown.org/"
                    AO3WebView(
                        url = lastUrl,
                        modifier = Modifier.padding(innerPadding),
                        onWebViewCreated = { webViewRef = it },
                        onCanGoBackChanged = { canGoBackInBrowser = it }
                    )
                }
            }
            composable(Screen.Downloads.route) {
                DownloadsScreen(
                    navController = navController,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            composable(
                route = "reader/{filePath}",
                arguments = listOf(navArgument("filePath") { type = NavType.StringType })
            ) { backStackEntry ->
                val encodedPath = backStackEntry.arguments?.getString("filePath") ?: ""
                val filePath = URLDecoder.decode(encodedPath, StandardCharsets.UTF_8.toString())
                EpubReaderScreen(File(filePath), navController)
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AO3WebView(
    url: String,
    modifier: Modifier = Modifier,
    onWebViewCreated: (WebView) -> Unit = {},
    onCanGoBackChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    var webView: WebView? by remember { mutableStateOf(null) }
    var canGoBack by remember { mutableStateOf(false) }

    // This handles the back gesture/button for the WebView
    BackHandler(enabled = canGoBack) {
        webView?.goBack()
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        CookieManager.getInstance().flush()
                        url?.let {
                            ctx.getSharedPreferences("ao3_prefs", Context.MODE_PRIVATE)
                                .edit().putString("last_url", it).apply()
                        }
                        val canBack = view?.canGoBack() ?: false
                        canGoBack = canBack
                        onCanGoBackChanged(canBack)
                    }

                    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                        super.doUpdateVisitedHistory(view, url, isReload)
                        val canBack = view?.canGoBack() ?: false
                        canGoBack = canBack
                        onCanGoBackChanged(canBack)
                    }

                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val requestUrl = request?.url?.toString() ?: ""
                        return if (requestUrl.contains("archiveofourown.org")) {
                            false
                        } else {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(requestUrl))
                                ctx.startActivity(intent)
                                true
                            } catch (e: Exception) {
                                false
                            }
                        }
                    }
                }

                setDownloadListener { downloadUrl, userAgent, contentDisposition, mimetype, _ ->
                    val fileName = URLUtil.guessFileName(downloadUrl, contentDisposition, mimetype)
                    val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                        val cookie = CookieManager.getInstance().getCookie(downloadUrl)
                        addRequestHeader("Cookie", cookie)
                        addRequestHeader("User-Agent", userAgent)
                        setTitle(fileName)
                        setDescription("Downloading from AO3...")
                        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        setDestinationInExternalFilesDir(ctx, Environment.DIRECTORY_DOWNLOADS, fileName)
                    }
                    
                    val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    dm.enqueue(request)
                    Toast.makeText(ctx, "Download started: $fileName", Toast.LENGTH_SHORT).show()
                }
                
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    @Suppress("DEPRECATION")
                    databaseEnabled = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                }

                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                loadUrl(url)
                onWebViewCreated(this)
                webView = this
            }
        },
        update = {
            // No-op to avoid reloading on every recomposition
        }
    )
}

enum class SortOrder { NAME, DATE, SIZE }

@Composable
fun DownloadsScreen(navController: NavController, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val baseDir = remember { context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!! }
    var currentDir by remember { mutableStateOf(baseDir) }
    var items by remember { mutableStateOf(listOf<File>()) }
    var fileToDelete by remember { mutableStateOf<File?>(null) }
    var fileToMove by remember { mutableStateOf<File?>(null) }
    var isOrganizing by remember { mutableStateOf(false) }
    
    // Search and Filter State
    var searchQuery by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf(SortOrder.NAME) }
    var showSortMenu by remember { mutableStateOf(false) }
    var isSearchExpanded by remember { mutableStateOf(false) }

    fun refreshFiles() {
        val allItems = currentDir.listFiles()?.toList() ?: emptyList()
        val filtered = if (searchQuery.isBlank()) {
            allItems
        } else {
            allItems.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
        
        items = filtered.sortedWith { f1, f2 ->
            if (f1.isDirectory && !f2.isDirectory) -1
            else if (!f1.isDirectory && f2.isDirectory) 1
            else {
                when (sortOrder) {
                    SortOrder.NAME -> f1.name.lowercase().compareTo(f2.name.lowercase())
                    SortOrder.DATE -> f2.lastModified().compareTo(f1.lastModified())
                    SortOrder.SIZE -> f2.length().compareTo(f1.length())
                }
            }
        }
    }

    LaunchedEffect(currentDir, searchQuery, sortOrder) {
        refreshFiles()
    }

    BackHandler(enabled = currentDir != baseDir || isSearchExpanded) {
        if (isSearchExpanded) {
            isSearchExpanded = false
            searchQuery = ""
        } else {
            currentDir = currentDir.parentFile ?: baseDir
        }
    }

    fun organizeFiles() {
        isOrganizing = true
        val allFiles = baseDir.walkTopDown().filter { it.isFile && it.extension.lowercase() == "epub" }.toList()
        var movedCount = 0
        
        // Regex to match "Series: Part (#) of (Series Name)"
        val seriesRegex = Regex("Series:\\s*Part\\s*(\\d+)\\s*of\\s*([^\\n\\r<]+)", RegexOption.IGNORE_CASE)

        allFiles.forEach { file ->
            try {
                FileInputStream(file).use { fis ->
                    val book = EpubReader().readEpub(fis)
                    val author = book.metadata.authors.firstOrNull()?.let { "${it.firstname} ${it.lastname}".trim() } ?: "Unknown Author"
                    
                    var seriesName: String? = null
                    
                    // Look through all resources in the book to find the text "Series: Part ..."
                    for (resource in book.contents) {
                        val text = String(resource.data, StandardCharsets.UTF_8)
                        // Strip HTML tags for cleaner searching
                        val cleanText = text.replace(Regex("<[^>]*>"), " ")
                        val match = seriesRegex.find(cleanText)
                        if (match != null) {
                            seriesName = match.groupValues[2].trim()
                            break
                        }
                    }

                    // Clean folder names
                    val cleanAuthor = author.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                    val targetDir = if (!seriesName.isNullOrBlank()) {
                        val cleanSeries = seriesName.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                        File(baseDir, "$cleanAuthor/$cleanSeries")
                    } else {
                        File(baseDir, cleanAuthor)
                    }
                    
                    if (!targetDir.exists()) targetDir.mkdirs()
                    
                    val targetFile = File(targetDir, file.name)
                    if (file.absolutePath != targetFile.absolutePath) {
                        if (file.renameTo(targetFile)) {
                            movedCount++
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        isOrganizing = false
        refreshFiles()
        Toast.makeText(context, "Organized $movedCount files", Toast.LENGTH_SHORT).show()
    }

    if (fileToDelete != null) {
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("Delete ${if (fileToDelete?.isDirectory == true) "Folder" else "Work"}") },
            text = { Text("Are you sure you want to delete '${fileToDelete?.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        fileToDelete?.deleteRecursively()
                        fileToDelete = null
                        refreshFiles()
                        Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (isSearchExpanded) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                placeholder = { Text("Search files...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { 
                        isSearchExpanded = false
                        searchQuery = ""
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Close search")
                    }
                },
                singleLine = true
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Downloads",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    if (currentDir != baseDir) {
                        val pathDisplay = currentDir.absolutePath.removePrefix(baseDir.absolutePath)
                        if (pathDisplay.isNotEmpty()) {
                            Text(
                                text = pathDisplay,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                
                IconButton(onClick = { isSearchExpanded = true }) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
                
                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Sort")
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sort by Name") },
                            onClick = { sortOrder = SortOrder.NAME; showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Sort by Date") },
                            onClick = { sortOrder = SortOrder.DATE; showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Sort by Size") },
                            onClick = { sortOrder = SortOrder.SIZE; showSortMenu = false }
                        )
                    }
                }

                if (fileToMove != null) {
                    IconButton(onClick = {
                        val targetFile = File(currentDir, fileToMove?.name ?: "")
                        if (fileToMove?.renameTo(targetFile) == true) {
                            Toast.makeText(context, "Moved ${fileToMove?.name}", Toast.LENGTH_SHORT).show()
                            fileToMove = null
                            refreshFiles()
                        } else {
                            Toast.makeText(context, "Error moving file", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste Here")
                    }
                    IconButton(onClick = { fileToMove = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel Move")
                    }
                } else {
                    IconButton(onClick = { organizeFiles() }, enabled = !isOrganizing) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = "Organize")
                    }
                }
            }
        }
        
        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (searchQuery.isNotEmpty()) "No results matching '$searchQuery'" else "No files found.")
            }
        } else {
            LazyColumn {
                if (currentDir != baseDir && !isSearchExpanded) {
                    item {
                        ListItem(
                            headlineContent = { Text("..") },
                            leadingContent = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) },
                            modifier = Modifier.clickable { currentDir = currentDir.parentFile ?: baseDir }
                        )
                        HorizontalDivider()
                    }
                }
                
                items(items) { item ->
                    ListItem(
                        headlineContent = { Text(item.name) },
                        supportingContent = { 
                            if (!item.isDirectory) {
                                Text("${item.length() / 1024} KB") 
                            }
                        },
                        leadingContent = {
                            Icon(
                                if (item.isDirectory) Icons.Default.Folder else Icons.Default.Download,
                                contentDescription = null,
                                tint = if (item == fileToMove) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { fileToMove = item }) {
                                    Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = "Move")
                                }
                                IconButton(onClick = { fileToDelete = item }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                                }
                            }
                        },
                        modifier = Modifier.clickable {
                            if (item.isDirectory) {
                                currentDir = item
                                isSearchExpanded = false
                                searchQuery = ""
                            } else {
                                if (item.extension.lowercase() == "epub") {
                                    val encodedPath = URLEncoder.encode(item.absolutePath, StandardCharsets.UTF_8.toString())
                                    navController.navigate("reader/$encodedPath")
                                } else {
                                    openFile(context, item)
                                }
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpubReaderScreen(file: File, navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val prefs = remember { context.getSharedPreferences("ao3_prefs", Context.MODE_PRIVATE) }
    val scrollKey = "scroll_${file.absolutePath}"
    
    var htmlContent by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("Reading...") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // Helper function to save scroll
    fun saveScroll(wv: WebView?) {
        wv?.let {
            val scrollY = it.scrollY
            if (scrollY > 0) {
                prefs.edit().putInt(scrollKey, scrollY).apply()
            }
        }
    }

    // Save scroll position on pause or dispose
    DisposableEffect(lifecycleOwner, file.absolutePath, webViewRef) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                saveScroll(webViewRef)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            saveScroll(webViewRef)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(file) {
        try {
            val book: Book = EpubReader().readEpub(FileInputStream(file))
            title = book.title ?: file.name
            val sb = StringBuilder()
            // Updated style for dark mode
            sb.append("<html><head><style>")
            sb.append("body { font-family: sans-serif; padding: 16px; line-height: 1.6; color: #E0E0E0; background-color: #121212; }")
            sb.append("hr { border: 0; border-top: 1px solid #333; margin: 20px 0; }")
            sb.append("a { color: #BB86FC; }")
            sb.append("</style></head><body>")
            
            book.contents.forEach { resource: Resource ->
                val content = String(resource.data, StandardCharsets.UTF_8)
                val bodyContent = if (content.contains("<body")) {
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
            htmlContent = sb.toString()
        } catch (e: Exception) {
            htmlContent = "<html><body style='background-color:#121212; color:white;'><h3>Error loading EPUB</h3><p>${e.message}</p></body></html>"
        }
    }

    Scaffold(
        containerColor = Color(0xFF121212), // Match HTML background
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1F1F1F),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                windowInsets = TopAppBarDefaults.windowInsets
            )
        }
    ) { padding ->
        AndroidView(
            modifier = Modifier.padding(padding).fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    setBackgroundColor(android.graphics.Color.parseColor("#121212"))
                    settings.javaScriptEnabled = true
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            val lastScroll = prefs.getInt(scrollKey, 0)
                            if (lastScroll > 0) {
                                // Try scrolling after several delays to ensure layout is done
                                view?.postDelayed({ view.scrollTo(0, lastScroll) }, 100)
                                view?.postDelayed({ view.scrollTo(0, lastScroll) }, 300)
                                view?.postDelayed({ view.scrollTo(0, lastScroll) }, 600)
                            }
                        }
                    }
                    setOnScrollChangeListener { _, _, _, scrollY, _ ->
                        if (scrollY > 0) {
                            prefs.edit().putInt(scrollKey, scrollY).apply()
                        }
                    }
                    webViewRef = this
                }
            },
            update = { webView ->
                // Avoid reloading if content is the same
                if (webView.tag != htmlContent && htmlContent.isNotBlank()) {
                    webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                    webView.tag = htmlContent
                }
            }
        )
    }
}

fun openFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, context.contentResolver.getType(uri))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No app found to open this file", Toast.LENGTH_SHORT).show()
    }
}
