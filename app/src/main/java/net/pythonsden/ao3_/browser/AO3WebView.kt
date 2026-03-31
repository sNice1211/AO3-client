package net.pythonsden.ao3_.browser

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AO3WebView(
    url: String,
    modifier: Modifier = Modifier,
    onUrlChanged: (String) -> Unit = {},
    onCanGoBackChanged: (Boolean) -> Unit = {},
    onWebViewCreated: (WebView) -> Unit = {}
) {
    var webView: WebView? by remember { mutableStateOf(null) }
    var canGoBack by remember { mutableStateOf(false) }

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
                        url?.let { onUrlChanged(it) }
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
                    // Safe settings
                    allowFileAccess = false
                    allowContentAccess = false
                    @Suppress("DEPRECATION")
                    databaseEnabled = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    // Minimize mixed content
                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                }

                // Only enable third-party cookies if absolutely necessary. 
                // AO3 might need them for some features, but let's try disabling or keeping it as it was if it worked.
                // The original code had it enabled.
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                
                loadUrl(url)
                onWebViewCreated(this)
                webView = this
            }
        },
        update = {
            // Usually we don't want to re-load on every update
        }
    )
}
