package com.example.ui.components

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.annotation.StringRes
import com.example.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.data.NewsArticle
import com.example.data.PublisherLinks
import java.util.Locale

/** The per-article actions every news list offers, wired once and passed down. */
class NewsActions(
    val open: (NewsArticle) -> Unit,
    val copyLink: (NewsArticle) -> Unit,
    val shareToWhatsApp: (NewsArticle) -> Unit,
    val toggleBookmark: (NewsArticle) -> Unit,
    val speak: (NewsArticle) -> Unit,
    val speakingLink: String?
)

/**
 * Builds [NewsActions]. [resolveLink] finds the newspaper's own address (never Google's or MSN's);
 * sharing and copying send only that address. When it cannot be found, the headline and the
 * paper's name go instead, so a story is never lost. [onOpen] shows the in-app reader.
 */
@Composable
fun rememberNewsActions(
    resolveLink: (NewsArticle, (String?) -> Unit) -> Unit,
    onOpen: (NewsArticle) -> Unit,
    onToggleBookmark: (NewsArticle) -> Unit
): NewsActions {
    val context = LocalContext.current
    val reader = rememberNewsReader()

    return NewsActions(
        open = onOpen,
        copyLink = { article ->
            resolveLink(article) { url ->
                if (url != null) {
                    context.copyToClipboard("News Link", url)
                    context.toast(R.string.toast_news_link_copied)
                } else {
                    context.copyToClipboard("News", PublisherLinks.fallbackText(article))
                    context.toast(R.string.toast_link_missing_copied_title)
                }
            }
        },
        shareToWhatsApp = { article ->
            resolveLink(article) { url -> context.shareToWhatsApp(url ?: PublisherLinks.fallbackText(article)) }
        },
        toggleBookmark = { article ->
            onToggleBookmark(article)
            context.toast(if (article.isSaved) R.string.toast_bookmark_removed else R.string.toast_bookmark_saved)
        },
        speak = { article -> reader.toggle(article, context) },
        speakingLink = reader.speakingLink
    )
}

/** Text-to-speech in Bengali (falls back to the device language), released with the composition. */
@Composable
private fun rememberNewsReader(): NewsReader {
    val context = LocalContext.current
    val reader = remember { NewsReader(context.applicationContext) }
    DisposableEffect(reader) { onDispose { reader.shutdown() } }
    return reader
}

private class NewsReader(context: Context) {
    var speakingLink by mutableStateOf<String?>(null)
        private set

    private val tts: TextToSpeech = TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) {
            val result = ttsOrNull?.setLanguage(Locale.forLanguageTag("bn-BD"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                ttsOrNull?.setLanguage(Locale.getDefault())
            }
        }
    }
    private val ttsOrNull: TextToSpeech? get() = runCatching { tts }.getOrNull()

    fun toggle(article: NewsArticle, context: Context) {
        if (speakingLink == article.link) {
            tts.stop()
            speakingLink = null
        } else {
            speakingLink = article.link
            tts.speak(context.getString(R.string.tts_article, article.source, article.title), TextToSpeech.QUEUE_FLUSH, null, article.link)
            context.toast(R.string.toast_reading_aloud)
        }
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}

fun Context.copyToClipboard(label: String, text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

fun Context.toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

fun Context.toast(@StringRes message: Int) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

/** Shares to WhatsApp when installed, otherwise through the system share sheet. */
fun Context.shareToWhatsApp(text: String) {
    val whatsApp = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        setPackage("com.whatsapp")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    try {
        startActivity(whatsApp)
    } catch (_: ActivityNotFoundException) {
        val any = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(any, getString(R.string.action_share_to)))
    }
}
