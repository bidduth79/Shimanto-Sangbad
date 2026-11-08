package com.example.ui.components

import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R

private const val MOBILE_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"

/** Full-screen in-app browser for reading an article or a web search, with reload and open-externally. */
@Composable
fun ArticleReaderDialog(
    url: String,
    title: String,
    onDismiss: () -> Unit,
    onOpenExternal: (String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    var isLoading by remember { mutableStateOf(true) }
    var currentUrl by remember { mutableStateOf(url) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            webView?.apply {
                stopLoading()
                clearHistory()
                destroy()
            }
            webView = null
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = scheme.surface) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(color = scheme.surfaceVariant, tonalElevation = 4.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_close), tint = scheme.onSurface)
                        }
                        Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                            Text(
                                text = title.ifBlank { stringResource(R.string.reader_default_title) },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = scheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(text = currentUrl, style = MaterialTheme.typography.labelSmall, color = scheme.outline, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { webView?.reload() }) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.action_reload), tint = scheme.primary)
                        }
                        IconButton(onClick = { onOpenExternal(currentUrl) }) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = stringResource(R.string.action_open_in_browser), tint = scheme.primary)
                        }
                    }
                }

                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = scheme.primary)
                }

                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            settings.userAgentString = MOBILE_USER_AGENT
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, loadedUrl: String?) {
                                    isLoading = false
                                    if (!loadedUrl.isNullOrBlank()) currentUrl = loadedUrl
                                }
                            }
                            webChromeClient = WebChromeClient()
                            loadUrl(url)
                            webView = this
                        }
                    },
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
            }
        }
    }
}
