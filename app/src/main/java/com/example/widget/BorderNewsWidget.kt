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
import com.example.MainActivity
import com.example.R
import com.example.data.KeywordMatcher
import com.example.data.NewsAlerts
import com.example.data.NewsArticle
import com.example.data.NewsFilters
import com.example.data.NewsSourceKinds
import com.example.data.local.NewsDao
import com.example.data.settings.SettingsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/** Home-screen widget: the latest on-topic border headlines; a tap opens that story in the app. */
class BorderNewsWidget : GlanceAppWidget() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun newsDao(): NewsDao
        fun settingsRepository(): SettingsRepository
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val deps = EntryPointAccessors.fromApplication(context.applicationContext, Deps::class.java)
        val now = System.currentTimeMillis()
        // Today's stories only, like everywhere else in the app
        val headlines = deps.newsDao().getRecent(NewsFilters.startOfToday(now))
            .asSequence()
            .filter { it.timestamp <= now + 10 * 60_000 }
            // Only BGB and BSF stories (their aliases included: BGB, বর্ডার গার্ড, BSF…)
            .filter { KeywordMatcher.matches(it.title, WIDGET_KEYWORDS) }
            .distinctBy { NewsFilters.headlineKey(it.title) }
            .take(MAX_ITEMS)
            .toList()

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
            // Nothing here opens anything: the only control is each story's WhatsApp button
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
                            Column(modifier = GlanceModifier.defaultWeight()) {
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
                                    .clickable(actionStartActivity(whatsAppIntent(context, article)))
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * The WhatsApp button goes through [WhatsAppShareActivity], which opens WhatsApp's "send to"
     * screen with the headline and link (a widget cannot do that directly; see that class).
     */
    private fun whatsAppIntent(context: Context, article: NewsArticle): Intent =
        Intent(context, WhatsAppShareActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(WhatsAppShareActivity.EXTRA_TEXT, article.title + '\n' + (article.originalLink ?: article.link))
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

    private fun bengali(n: Long) = n.toString().map { if (it in '0'..'9') '০' + (it - '0') else it }.joinToString("")

    companion object {
        /** Every fresh story is listed; the cap only guards the system's size limit for widgets. */
        private const val MAX_ITEMS = 100
        private val WIDGET_KEYWORDS = listOf("বিজিবি", "বিএসএফ")
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
