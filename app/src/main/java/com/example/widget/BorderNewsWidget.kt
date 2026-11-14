package com.example.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.R
import com.example.data.NewsArticle
import com.example.data.NewsFilters
import com.example.data.NewsSourceKinds
import com.example.data.WidgetStories
import com.example.data.local.NewsDao
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Home-screen widget: today's BGB/BSF headlines. A headline tap opens the newspaper's article in
 * the browser; the green button sends its link to WhatsApp.
 */
class BorderNewsWidget : GlanceAppWidget() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun newsDao(): NewsDao
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val deps = EntryPointAccessors.fromApplication(context.applicationContext, Deps::class.java)
        val now = System.currentTimeMillis()
        // Today's BGB/BSF stories only (see WidgetStories)
        val headlines = WidgetStories.select(deps.newsDao().getRecent(NewsFilters.startOfToday(now)), now)

        provideContent { WidgetContent(context, headlines, now) }
    }

    @Composable
    private fun WidgetContent(context: Context, headlines: List<NewsArticle>, now: Long) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(BACKGROUND)
                .cornerRadius(22.dp)
                .padding(14.dp)
        ) {
            // Header: no action. Each story: headline opens the newspaper, button shares its link
            Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
                Image(ImageProvider(R.drawable.bgb_emblem_header), contentDescription = null, modifier = GlanceModifier.size(28.dp))
                Spacer(GlanceModifier.width(8.dp))
                Column {
                    Text(context.getString(R.string.app_name), style = TextStyle(color = WHITE, fontSize = 15.sp, fontWeight = FontWeight.Bold))
                    Text(context.getString(R.string.widget_subtitle), style = TextStyle(color = GOLD, fontSize = 11.sp))
                }
            }
            Spacer(GlanceModifier.height(8.dp))
            if (headlines.isEmpty()) {
                Text(context.getString(R.string.widget_empty), style = TextStyle(color = MUTED, fontSize = 13.sp))
            } else {
                LazyColumn {
                    items(headlines, itemId = { it.link.hashCode().toLong() }) { article ->
                        Row(
                            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Tapping the headline opens the newspaper's article in the browser
                            Column(
                                modifier = GlanceModifier
                                    .defaultWeight()
                                    .clickable(actionStartActivity(storyIntent(context, article, WidgetLinkActivity.ACTION_OPEN)))
                            ) {
                                Text(
                                    article.title,
                                    maxLines = 2,
                                    style = TextStyle(color = WHITE, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                )
                                Text(
                                    "${NewsSourceKinds.publisherOf(article)} • ${ago(context, article.timestamp, now)}",
                                    maxLines = 1,
                                    style = TextStyle(color = MUTED, fontSize = 11.sp)
                                )
                            }
                            Spacer(GlanceModifier.width(6.dp))
                            Image(
                                provider = ImageProvider(R.drawable.ic_widget_send),
                                contentDescription = context.getString(R.string.news_share_whatsapp),
                                modifier = GlanceModifier
                                    .size(40.dp)
                                    .cornerRadius(20.dp)
                                    .background(BUTTON)
                                    .padding(9.dp)
                                    .clickable(actionStartActivity(storyIntent(context, article, WidgetLinkActivity.ACTION_SHARE)))
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Both widget taps go through [WidgetLinkActivity], which finds the newspaper's own address:
     * [WidgetLinkActivity.ACTION_OPEN] opens it in the browser, [WidgetLinkActivity.ACTION_SHARE]
     * sends only that address to WhatsApp.
     */
    private fun storyIntent(context: Context, article: NewsArticle, action: String): Intent =
        Intent(context, WidgetLinkActivity::class.java).apply {
            this.action = action
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(WidgetLinkActivity.EXTRA_LINK, article.link)
            putExtra(WidgetLinkActivity.EXTRA_TITLE, article.title)
        }

    /**
     * Clock time ("রাত ১১:২০"), or day and time when older than today. A widget only redraws after
     * a refresh, so "5 minutes ago" would be wrong a few minutes later; a clock time never is.
     */
    private fun ago(context: Context, timestamp: Long, now: Long): String {
        val zone = java.util.TimeZone.getDefault()
        val sameDay = (timestamp + zone.getOffset(timestamp)) / DAY_MILLIS == (now + zone.getOffset(now)) / DAY_MILLIS
        val pattern = if (sameDay) "a h:mm" else "d MMM, a h:mm"
        return java.text.SimpleDateFormat(pattern, java.util.Locale.forLanguageTag("bn-BD")).format(java.util.Date(timestamp))
    }

    companion object {
        private val BUTTON = ColorProvider(Color(0x3325D366))
        private const val DAY_MILLIS = 24L * 60 * 60 * 1000
        private val BACKGROUND = ColorProvider(Color(0xF00A0F1C))
        private val WHITE = ColorProvider(Color(0xFFF1F5F9))
        private val MUTED = ColorProvider(Color(0xFF94A3B8))
        private val GOLD = ColorProvider(Color(0xFFFBBF24))

        /** Redraws every placed widget, e.g. after a news refresh. */
        suspend fun refreshAll(context: Context) {
            runCatching { BorderNewsWidget().updateAll(context) }
        }
    }
}

class BorderNewsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BorderNewsWidget()
}
