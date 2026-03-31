package net.pythonsden.ao3_.reader

import android.view.ViewTreeObserver
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import net.pythonsden.ao3_.viewmodel.ReaderUiState
import net.pythonsden.ao3_.viewmodel.ReaderViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    file: File,
    viewModel: ReaderViewModel,
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(file) {
        viewModel.loadEpub(file)
    }

    Scaffold(
        containerColor = Color(0xFF121212),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (val state = uiState) {
                            is ReaderUiState.Success -> state.title
                            is ReaderUiState.Loading -> "Loading..."
                            else -> "Reader"
                        },
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1F1F1F),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                is ReaderUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ReaderUiState.Success -> {
                    var hasRestoredScroll by remember(file.absolutePath) { mutableStateOf(false) }
                    
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                setBackgroundColor(android.graphics.Color.parseColor("#121212"))
                                settings.javaScriptEnabled = false
                                settings.allowFileAccess = false
                                settings.allowContentAccess = false
                                
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        if (!hasRestoredScroll && state.initialScroll > 0) {
                                            view?.scrollTo(0, state.initialScroll)
                                        }
                                    }
                                }
                                
                                val layoutListener = object : ViewTreeObserver.OnGlobalLayoutListener {
                                    override fun onGlobalLayout() {
                                        if (!hasRestoredScroll && state.initialScroll > 0) {
                                            if (contentHeight > 0) {
                                                scrollTo(0, state.initialScroll)
                                                // If we've scrolled to or past the target, or reached bottom
                                                if (scrollY >= state.initialScroll || scrollY > 0) {
                                                    hasRestoredScroll = true
                                                }
                                            }
                                        } else if (state.initialScroll == 0) {
                                            hasRestoredScroll = true
                                        }
                                    }
                                }
                                viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
                                
                                setOnScrollChangeListener { _, _, _, scrollY, _ ->
                                    if (hasRestoredScroll) {
                                        viewModel.saveScrollPosition(file.absolutePath, scrollY)
                                    }
                                }
                            }
                        },
                        update = { webView ->
                            if (webView.tag != state.content) {
                                webView.loadDataWithBaseURL(null, state.content, "text/html", "UTF-8", null)
                                webView.tag = state.content
                            }
                        }
                    )
                }
                is ReaderUiState.Error -> {
                    Text(
                        text = "Error: ${state.message}",
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}
