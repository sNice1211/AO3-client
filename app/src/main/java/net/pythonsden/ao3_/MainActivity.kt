package net.pythonsden.ao3_

import android.os.Bundle
import android.webkit.CookieManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import net.pythonsden.ao3_.browser.AO3WebView
import net.pythonsden.ao3_.downloads.DownloadsScreen
import net.pythonsden.ao3_.reader.ReaderScreen
import net.pythonsden.ao3_.ui.theme.AO3clientTheme
import net.pythonsden.ao3_.viewmodel.BrowserViewModel
import net.pythonsden.ao3_.viewmodel.DownloadsViewModel
import net.pythonsden.ao3_.viewmodel.MainViewModel
import net.pythonsden.ao3_.viewmodel.ReaderViewModel
import net.pythonsden.ao3_.viewmodel.ViewModelFactory
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupCookieManager()
        enableEdgeToEdge()
        setContent {
            val app = application as AO3Application
            val factory = ViewModelFactory(
                app.settingsRepository,
                app.fileRepository,
                app.epubRepository
            )
            AO3clientTheme {
                MainScreen(factory)
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
fun MainScreen(factory: ViewModelFactory) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    val mainViewModel: MainViewModel = viewModel(factory = factory)
    val browserViewModel: BrowserViewModel = viewModel(factory = factory)
    val downloadsViewModel: DownloadsViewModel = viewModel(factory = factory)
    val readerViewModel: ReaderViewModel = viewModel(factory = factory)

    val isOfflineMode by mainViewModel.offlineMode.collectAsState()
    val lastRoute by mainViewModel.lastRoute.collectAsState()
    val lastEpubPath by mainViewModel.lastEpubPath.collectAsState()
    val canGoBackInBrowser by browserViewModel.canGoBack.collectAsState()

    // Handle Restoration
    var isRestored by remember { mutableStateOf(false) }
    LaunchedEffect(lastRoute, isOfflineMode) {
        if (!isRestored && lastRoute != null) {
            val startRoute = if (isOfflineMode) Screen.Downloads.route else Screen.Browser.route
            if (lastRoute != startRoute) {
                if (lastRoute == "reader/{filePath}") {
                    lastEpubPath?.let { path ->
                        val encoded = URLEncoder.encode(path, StandardCharsets.UTF_8.toString())
                        navController.navigate("reader/$encoded")
                    }
                } else if (!isOfflineMode || lastRoute == Screen.Downloads.route) {
                    navController.navigate(lastRoute!!) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
            isRestored = true
        }
    }

    // Save current route
    LaunchedEffect(navBackStackEntry) {
        navBackStackEntry?.destination?.route?.let { route ->
            mainViewModel.updateLastRoute(route)
        }
    }

    Scaffold(
        topBar = {
            if (currentDestination?.route != "reader/{filePath}") {
                TopAppBar(
                    title = { Text("AO3 Client") },
                    navigationIcon = {
                        if (currentDestination?.route == Screen.Browser.route && canGoBackInBrowser) {
                            // This would ideally be handled by a message to the browser component
                            // For now, Browser handles back via BackHandler internally
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
                                    mainViewModel.toggleOfflineMode(checked)
                                    if (checked && currentDestination?.route == Screen.Browser.route) {
                                        navController.navigate(Screen.Downloads.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
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
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = screen.icon,
                            label = { Text(screen.label) },
                            selected = selected,
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
                    val lastUrl by browserViewModel.lastUrl.collectAsState()
                    AO3WebView(
                        url = lastUrl,
                        modifier = Modifier.padding(innerPadding),
                        onUrlChanged = { browserViewModel.updateLastUrl(it) },
                        onCanGoBackChanged = { browserViewModel.setCanGoBack(it) }
                    )
                }
            }
            composable(Screen.Downloads.route) {
                DownloadsScreen(
                    viewModel = downloadsViewModel,
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
                ReaderScreen(File(filePath), readerViewModel, navController)
            }
        }
    }
}
