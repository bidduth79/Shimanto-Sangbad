package com.example.ui

import android.content.Intent
import android.net.Uri
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.YouTubeSearchResult
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun VideoList(
    videos: List<YouTubeSearchResult>,
    errorMessage: String? = null,
    onRetry: (() -> Unit)? = null
) {
    val uriHandler = LocalUriHandler.current
    val scheme = MaterialTheme.colorScheme
    var activePlayingVideo by remember { mutableStateOf<YouTubeSearchResult?>(null) }

    if (activePlayingVideo != null) {
        InAppVideoPlayerDialog(
            video = activePlayingVideo!!,
            onDismiss = { activePlayingVideo = null },
            onOpenInExternalApp = { videoId ->
                try {
                    uriHandler.openUri("https://www.youtube.com/watch?v=$videoId")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        )
    }

    if (!errorMessage.isNullOrBlank() && videos.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Error",
                        tint = scheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "ভিডিও লোড হয়নি",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        fontSize = 14.sp,
                        color = scheme.outline,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (onRetry != null) {
                        Button(
                            onClick = onRetry,
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = scheme.primary,
                                contentColor = scheme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("পুনরায় চেষ্টা করুন")
                        }
                    }
                }
            }
        }
    } else if (videos.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.VideoLibrary,
                    contentDescription = null,
                    tint = scheme.outline,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "কোনো ভিডিও পাওয়া যায়নি।", color = scheme.outline, fontSize = 15.sp)
            }
        }
    } else {
        var selectedHoursFilter by remember { mutableIntStateOf(48) } // 24 = 24h, 48 = 48h, 168 = 7d
        val timeFilteredVideos = remember(videos, selectedHoursFilter) {
            val now = System.currentTimeMillis()
            val cutoff = now - (selectedHoursFilter.toLong() * 3_600_000L)
            val filtered = videos.filter { video ->
                val ts = parseYouTubeTimestampMillis(video.snippet?.publishedAt)
                ts <= 0L || ts in (cutoff)..(now + 600_000L)
            }
            if (filtered.isEmpty() && selectedHoursFilter == 48) {
                videos
            } else {
                filtered
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val filterOptions = listOf(
                    24 to "আজকের (২৪ ঘণ্টা)",
                    48 to "সাম্প্রতিক (৪৮ ঘণ্টা)",
                    168 to "সপ্তাহের (৭ দিন)"
                )
                filterOptions.forEach { (hours, label) ->
                    val isSelected = selectedHoursFilter == hours
                    Surface(
                        color = if (isSelected) scheme.primary else scheme.surfaceVariant,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { selectedHoursFilter = hours }
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) scheme.onPrimary else scheme.onBackground,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            if (timeFilteredVideos.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = null,
                            tint = scheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "নির্বাচিত সময়ে কোনো নতুন ভিডিও পাওয়া যায়নি।",
                            color = scheme.outline,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(timeFilteredVideos, key = { it.id?.videoId ?: it.hashCode().toString() }) { video ->
                        val videoId = video.id?.videoId
                        if (!videoId.isNullOrBlank()) {
                            VideoCard(
                                video = video,
                                onInAppPlay = { activePlayingVideo = video },
                                onExternalPlay = {
                                    val url = "https://www.youtube.com/watch?v=$videoId"
                                    try {
                                        uriHandler.openUri(url)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoCard(
    video: YouTubeSearchResult,
    onInAppPlay: () -> Unit,
    onExternalPlay: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val snippet = video.snippet
    val title = snippet?.title ?: "শিরোনামহীন ভিডিও"
    val channelTitle = snippet?.channelTitle ?: ""
    val publishedAt = snippet?.publishedAt
    val formattedTime = formatYouTubeDate(publishedAt)
    val videoId = video.id?.videoId ?: ""
    val thumbnailUrl = snippet?.thumbnails?.high?.url
        ?: snippet?.thumbnails?.medium?.url
        ?: snippet?.thumbnails?.default?.url
        ?: if (videoId.isNotBlank()) "https://i.ytimg.com/vi/$videoId/hqdefault.jpg" else ""

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onInAppPlay),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surface)
    ) {
        Column {
            if (thumbnailUrl.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(thumbnailUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    )
                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "প্লে",
                                tint = scheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (channelTitle.isNotBlank()) {
                        Text(
                            text = channelTitle,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = scheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (channelTitle.isNotBlank() && formattedTime.isNotBlank()) {
                        Text(
                            text = " • ",
                            fontSize = 13.sp,
                            color = scheme.outline
                        )
                    }
                    if (formattedTime.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "সময়",
                                tint = scheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formattedTime,
                                fontSize = 12.sp,
                                color = scheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onExternalPlay,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "ইউটিউব", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    val context = LocalContext.current
                    val clipboardManager = LocalClipboardManager.current
                    IconButton(
                        onClick = {
                            if (videoId.isNotBlank()) {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    setPackage("com.whatsapp")
                                    putExtra(Intent.EXTRA_TEXT, "$title\nhttps://www.youtube.com/watch?v=$videoId")
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "আপনার ফোনে WhatsApp ইনস্টল করা নেই", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Share to WhatsApp",
                            tint = Color(0xFF25D366),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            if (videoId.isNotBlank()) {
                                clipboardManager.setText(AnnotatedString("https://www.youtube.com/watch?v=$videoId"))
                                Toast.makeText(context, "✅ ভিডিও লিংক কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "লিংক কপি করুন",
                            tint = scheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

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
    val title = snippet?.title ?: "ইউটিউব ভিডিও"
    val channelTitle = snippet?.channelTitle ?: ""

    // useDirectWatchMode = false uses Smart IFrame API (with auto-fallback), true uses direct m.youtube.com player
    var useDirectWatchMode by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = scheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ইন-অ্যাপ ভিডিও প্লেয়ার",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onBackground
                        )
                        Text(
                            text = if (useDirectWatchMode) "মোড: ডাইরেক্ট প্লেয়ার" else "মোড: স্মার্ট এমবেড প্লেয়ার",
                            fontSize = 11.sp,
                            color = scheme.primary
                        )
                    }

                    // Player Mode Switch Button in case a channel restricts embed
                    FilledTonalButton(
                        onClick = {
                            isLoading = true
                            useDirectWatchMode = !useDirectWatchMode
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (useDirectWatchMode) "এমবেড মোড" else "ডাইরেক্ট মোড",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "বন্ধ করুন",
                            tint = scheme.onBackground
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

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
                            .height(if (useDirectWatchMode) 300.dp else 220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black)
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
                                        databaseEnabled = true
                                        mediaPlaybackRequiresUserGesture = false
                                        loadsImagesAutomatically = true
                                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
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
                                            // Keep YouTube links inside the In-App WebView instead of launching external intents
                                            if (reqUrl.startsWith("intent://") || reqUrl.startsWith("vnd.youtube:")) {
                                                return true
                                            }
                                            return false
                                        }

                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            isLoading = false
                                            // If in direct watch mode, inject CSS to hide top banner and focus on the video player
                                            if (url?.contains("m.youtube.com/watch") == true) {
                                                view?.evaluateJavascript(
                                                    """
                                                    (function() {
                                                        var style = document.createElement('style');
                                                        style.innerHTML = 'ytm-mobile-topbar-renderer, .watch-below-the-player, ytm-item-section-renderer, ytm- mealbar-promo-renderer { display: none !important; }';
                                                        document.head.appendChild(style);
                                                        var v = document.querySelector('video');
                                                        if (v) { v.muted = false; v.play(); }
                                                    })();
                                                    """.trimIndent(),
                                                    null
                                                )
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

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (channelTitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = channelTitle,
                        fontSize = 13.sp,
                        color = scheme.outline
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                val clipboardManager = LocalClipboardManager.current
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { onOpenInExternalApp(videoId) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = scheme.primary,
                            contentColor = scheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ইউটিউব অ্যাপ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                setPackage("com.whatsapp")
                                putExtra(Intent.EXTRA_TEXT, "https://www.youtube.com/watch?v=$videoId")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "WhatsApp ইনস্টল করা নেই", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            tint = Color(0xFF25D366),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString("https://www.youtube.com/watch?v=$videoId"))
                            Toast.makeText(context, "✅ লিংক কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("কপি", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, title)
                                putExtra(Intent.EXTRA_TEXT, "$title\nhttps://www.youtube.com/watch?v=$videoId")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "শেয়ার করুন"))
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("শেয়ার", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

fun parseYouTubeTimestampMillis(publishedAt: String?): Long {
    if (publishedAt.isNullOrBlank()) return 0L
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        inputFormat.parse(publishedAt.take(19))?.time ?: 0L
    } catch (e: Exception) {
        0L
    }
}

fun formatYouTubeDate(publishedAt: String?): String {
    if (publishedAt.isNullOrBlank()) return ""
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val date = inputFormat.parse(publishedAt.take(19)) ?: return ""
        val timestamp = date.time

        val outputFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale("bn", "BD")).apply {
            timeZone = TimeZone.getTimeZone("Asia/Dhaka")
        }
        val formattedDate = outputFormat.format(date)

        val diff = System.currentTimeMillis() - timestamp
        val minutes = diff / (1000 * 60)
        val hours = minutes / 60
        val days = hours / 24

        val relativeTime = when {
            diff < 0 -> "সদ্যপ্রাপ্ত"
            minutes < 60 -> "${toBengaliNumber(minutes.toString())} মিনিট আগে"
            hours < 24 -> "${toBengaliNumber(hours.toString())} ঘণ্টা আগে"
            else -> "${toBengaliNumber(days.toString())} দিন আগে"
        }
        "$relativeTime • $formattedDate"
    } catch (e: Exception) {
        ""
    }
}

private fun toBengaliNumber(number: String): String {
    val bengaliDigits = arrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    val builder = StringBuilder()
    for (char in number) {
        if (char in '0'..'9') {
            builder.append(bengaliDigits[char - '0'])
        } else {
            builder.append(char)
        }
    }
    return builder.toString()
}
