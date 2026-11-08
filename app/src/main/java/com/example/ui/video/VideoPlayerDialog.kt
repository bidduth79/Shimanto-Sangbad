package com.example.ui.video

import com.example.ui.components.copyToClipboard
import com.example.ui.components.shareToWhatsApp
import com.example.ui.components.toast
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import android.content.Intent
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.YouTubeSearchResult
import androidx.compose.ui.res.stringResource
import com.example.R

// In-app YouTube player (embed with automatic fallback to the mobile watch page).

/**
 * Builds the HTML payload for YouTube IFrame API with valid origin/referer and automatic fallback
 * to mobile YouTube watch page if the channel blocks third-party embeds (Error 101 / 150 / 153).
 */
private fun buildYouTubePlayerHtml(videoId: String): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <meta name="referrer" content="strict-origin-when-cross-origin">
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; background-color: #000; }
                html, body { width: 100%; height: 100%; overflow: hidden; background: #000; }
                #player, iframe { width: 100%; height: 100%; border: none; display: block; }
            </style>
        </head>
        <body>
            <div id="player"></div>
            <script src="https://www.youtube.com/iframe_api"></script>
            <script>
                var player;
                function onYouTubeIframeAPIReady() {
                    player = new YT.Player('player', {
                        width: '100%',
                        height: '100%',
                        videoId: '$videoId',
                        playerVars: {
                            'autoplay': 1,
                            'playsinline': 1,
                            'controls': 1,
                            'rel': 0,
                            'modestbranding': 1,
                            'fs': 1,
                            'origin': 'https://www.youtube.com'
                        },
                        events: {
                            'onReady': function(event) {
                                event.target.playVideo();
                            },
                            'onError': function(event) {
                                // Error 101 or 150 means the channel owner disabled external embedding.
                                // Immediately load mobile YouTube watch page inside this WebView!
                                window.location.replace('https://m.youtube.com/watch?v=$videoId&autoplay=1');
                            }
                        }
                    });
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}

@Composable
fun InAppVideoPlayerDialog(
    video: YouTubeSearchResult,
    onDismiss: () -> Unit,
    onOpenInExternalApp: (String) -> Unit
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val videoId = video.id?.videoId ?: ""
    val snippet = video.snippet
    val title = snippet?.title ?: stringResource(R.string.video_default_title)
    val channelTitle = snippet?.channelTitle ?: ""

    // useDirectWatchMode = false uses Smart IFrame API (with auto-fallback), true uses direct m.youtube.com player
    var useDirectWatchMode by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }


    // Only the video: a 16:9 player with two small round buttons on top (switch player mode, close)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.Black)
        ) {
            if (videoId.isNotBlank()) {
                var playerWebView by remember { mutableStateOf<WebView?>(null) }

                DisposableEffect(videoId) {
                    onDispose {
                        playerWebView?.stopLoading()
                        playerWebView?.loadUrl("about:blank")
                        playerWebView?.destroy()
                        playerWebView = null
                    }
                }

                LaunchedEffect(useDirectWatchMode, videoId) {
                    val wv = playerWebView ?: return@LaunchedEffect
                    if (useDirectWatchMode) {
                        wv.loadUrl(
                            "https://m.youtube.com/watch?v=$videoId",
                            mapOf("Referer" to "https://www.youtube.com")
                        )
                    } else {
                        wv.loadDataWithBaseURL(
                            "https://www.youtube.com",
                            buildYouTubePlayerHtml(videoId),
                            "text/html",
                            "UTF-8",
                            "https://www.youtube.com"
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (useDirectWatchMode) Modifier.height(300.dp) else Modifier.aspectRatio(16f / 9f))
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                                CookieManager.getInstance().setAcceptCookie(true)
                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    mediaPlaybackRequiresUserGesture = false
                                    loadsImagesAutomatically = true
                                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                    useWideViewPort = true
                                    loadWithOverviewMode = true
                                    userAgentString =
                                        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
                                }

                                webChromeClient = WebChromeClient()
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        val reqUrl = request?.url?.toString() ?: return false
                                        // Keep YouTube links inside the WebView instead of launching external intents
                                        return reqUrl.startsWith("intent://") || reqUrl.startsWith("vnd.youtube:")
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        isLoading = false
                                        // Direct watch mode: hide the mobile site's chrome around the player
                                        if (url?.contains("m.youtube.com/watch") == true) {
                                            view?.evaluateJavascript(HIDE_MOBILE_CHROME_JS, null)
                                        }
                                    }
                                }

                                loadDataWithBaseURL(
                                    "https://www.youtube.com",
                                    buildYouTubePlayerHtml(videoId),
                                    "text/html",
                                    "UTF-8",
                                    "https://www.youtube.com"
                                )
                                playerWebView = this
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    if (isLoading) {
                        CircularProgressIndicator(
                            color = scheme.primary,
                            modifier = Modifier
                                .size(32.dp)
                                .align(Alignment.Center)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PlayerOverlayButton(
                    icon = Icons.Default.SwapHoriz,
                    description = stringResource(if (useDirectWatchMode) R.string.player_switch_to_embed else R.string.player_switch_to_direct)
                ) {
                    isLoading = true
                    useDirectWatchMode = !useDirectWatchMode
                }
                PlayerOverlayButton(icon = Icons.Default.Close, description = stringResource(R.string.action_close), onClick = onDismiss)
            }
        }
    }
}

private const val HIDE_MOBILE_CHROME_JS =
    "(function() {" +
        "var style = document.createElement('style');" +
        "style.innerHTML = 'ytm-mobile-topbar-renderer, .watch-below-the-player, ytm-item-section-renderer, ytm-mealbar-promo-renderer { display: none !important; }';" +
        "document.head.appendChild(style);" +
        "var v = document.querySelector('video');" +
        "if (v) { v.muted = false; v.play(); }" +
        "})();"

/** Small translucent round button drawn over the video. */
@Composable
private fun PlayerOverlayButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(34.dp)
            .background(Color.Black.copy(alpha = 0.55f), androidx.compose.foundation.shape.CircleShape)
    ) {
        Icon(icon, contentDescription = description, tint = Color.White, modifier = Modifier.size(18.dp))
    }
}
