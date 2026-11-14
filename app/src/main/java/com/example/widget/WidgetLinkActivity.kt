package com.example.widget

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.R
import com.example.data.NewsArticle
import com.example.data.NewsRepository
import com.example.data.PublisherLinks
import com.example.data.local.NewsDao
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Invisible stop between a widget tap and the outside world. Two jobs:
 *  - [ACTION_OPEN] (headline tapped): opens the newspaper's article in the browser,
 *  - [ACTION_SHARE] (WhatsApp button): sends WhatsApp only the newspaper's address.
 *
 * A widget cannot do either directly: a stored link may still be Google's or MSN's and needs
 * looking up first, and Glance stamps list item intents with a data URI that erases the
 * "text/plain" type WhatsApp's share screen is matched by. Links found ahead of time
 * ([NewsRepository.prepareWidgetLinks]) make this instant; otherwise it takes a moment.
 */
class WidgetLinkActivity : ComponentActivity() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun newsRepository(): NewsRepository
        fun newsDao(): NewsDao
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val link = intent?.getStringExtra(EXTRA_LINK)
        if (link.isNullOrBlank()) {
            finish()
            return
        }
        val deps = EntryPointAccessors.fromApplication(applicationContext, Deps::class.java)
        lifecycleScope.launch {
            val article = deps.newsDao().getByLink(link)
                ?: NewsArticle(link = link, title = intent.getStringExtra(EXTRA_TITLE).orEmpty(), pubDate = "", source = "", timestamp = 0L)
            // Tell the user something is happening if the lookup is not instant
            val hint = launch {
                delay(HINT_AFTER_MILLIS)
                Toast.makeText(this@WidgetLinkActivity, R.string.widget_link_searching, Toast.LENGTH_SHORT).show()
            }
            val url = withTimeoutOrNull(LOOKUP_TIMEOUT_MILLIS) {
                runCatching { deps.newsRepository().publisherLinkFor(article) }.getOrNull()
            }
            hint.cancel()
            when (intent.action) {
                ACTION_SHARE -> share(url ?: PublisherLinks.fallbackText(article))
                // Without the newspaper's address the stored one still leads there in a browser
                else -> open(url ?: (article.originalLink ?: article.link))
            }
            finish()
        }
    }

    private fun open(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.widget_browser_missing, Toast.LENGTH_SHORT).show()
        }
    }

    private fun share(text: String) {
        if (!sendTo(WHATSAPP, text) && !sendTo(WHATSAPP_BUSINESS, text)) {
            Toast.makeText(this, R.string.widget_whatsapp_missing, Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendTo(packageName: String, text: String): Boolean = try {
        startActivity(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage(packageName)
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

    companion object {
        const val ACTION_OPEN = "com.example.widget.OPEN_STORY"
        const val ACTION_SHARE = "com.example.widget.SHARE_STORY"
        const val EXTRA_LINK = "story_link"
        const val EXTRA_TITLE = "story_title"
        private const val WHATSAPP = "com.whatsapp"
        private const val WHATSAPP_BUSINESS = "com.whatsapp.w4b"
        private const val HINT_AFTER_MILLIS = 600L
        private const val LOOKUP_TIMEOUT_MILLIS = 12_000L
    }
}
