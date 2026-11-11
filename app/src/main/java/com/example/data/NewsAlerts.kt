package com.example.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.NewsDao
import com.example.data.settings.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "Breaking border news" notifications. After every refresh (in the app or in the background
 * worker) recent stories whose headline contains one of the alert words — and is also about the
 * user's main keywords, so "গুলি" alone does not alert on unrelated news — are announced once.
 */
@Singleton
class NewsAlerts @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val newsDao: NewsDao,
    private val settingsRepository: SettingsRepository
) {
    private val lock = Mutex()

    suspend fun checkAndNotify(now: Long = System.currentTimeMillis()) = lock.withLock {
        val settings = settingsRepository.current()
        if (!settings.alertsEnabled || settings.alertKeywords.isEmpty()) return@withLock

        val fresh = pickAlerts(
            recent = newsDao.getRecent(maxOf(now - WINDOW_MILLIS, NewsFilters.startOfToday(now))),
            alertKeywords = settings.alertKeywords,
            topicKeywords = settings.keywords,
            alreadyNotified = settingsRepository.notifiedLinks(),
            now = now
        )
        if (fresh.isEmpty()) return@withLock

        // The first check after alerts are switched on only takes note of what already exists,
        // so turning the feature on does not fire a burst of old stories.
        if (!settingsRepository.alertsPrimed()) {
            settingsRepository.addNotifiedLinks(fresh.map { it.link }, primed = true)
            return@withLock
        }
        settingsRepository.addNotifiedLinks(fresh.map { it.link }, primed = true)
        if (canPost()) post(fresh)
    }

    private fun canPost(): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    @Suppress("MissingPermission") // checked in canPost()
    private fun post(articles: List<NewsArticle>) {
        ensureChannel()
        val manager = NotificationManagerCompat.from(context)
        articles.take(MAX_SINGLE).forEach { article ->
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_monochrome)
                .setContentTitle(context.getString(R.string.alert_title))
                .setContentText(article.title)
                .setStyle(NotificationCompat.BigTextStyle().bigText(article.title))
                .setSubText(NewsSourceKinds.publisherOf(article))
                .setWhen(article.timestamp)
                .setShowWhen(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
                .setAutoCancel(true)
                .setGroup(GROUP)
                .setContentIntent(openIntent(article))
                .build()
            manager.notify(article.link.hashCode(), notification)
        }
        if (articles.size > 1) {
            val summary = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_monochrome)
                .setContentTitle(context.getString(R.string.alert_summary, articles.size))
                .setStyle(
                    NotificationCompat.InboxStyle().also { style -> articles.take(5).forEach { style.addLine(it.title) } }
                )
                .setGroup(GROUP)
                .setGroupSummary(true)
                .setAutoCancel(true)
                .setContentIntent(openIntent(null))
                .build()
            manager.notify(SUMMARY_ID, summary)
        }
    }

    /** Opens the app; with an article, straight into the reader for that story. */
    private fun openIntent(article: NewsArticle?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (article != null) {
                putExtra(EXTRA_LINK, article.link)
                putExtra(EXTRA_TITLE, article.title)
            }
        }
        return PendingIntent.getActivity(
            context,
            article?.link?.hashCode() ?: 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.alert_channel), NotificationManager.IMPORTANCE_HIGH).apply {
                description = context.getString(R.string.alert_channel_description)
            }
        )
    }

    companion object {
        const val EXTRA_LINK = "open_link"
        const val EXTRA_TITLE = "open_title"
        private const val CHANNEL_ID = "border_alerts"
        private const val GROUP = "border_alerts_group"
        private const val SUMMARY_ID = 7_001
        private const val MAX_SINGLE = 4
        /** Only stories from the last hours count as "breaking". */
        private const val WINDOW_MILLIS = 6L * 60 * 60 * 1000

        val DEFAULT_ALERT_KEYWORDS = listOf("সীমান্ত হত্যা", "পুশইন", "অনুপ্রবেশ", "গুলি")

        /** Pure selection rule, kept separate for tests. */
        fun pickAlerts(
            recent: List<NewsArticle>,
            alertKeywords: List<String>,
            topicKeywords: List<String>,
            alreadyNotified: Set<String>,
            now: Long
        ): List<NewsArticle> = recent
            .asSequence()
            .filter { it.link !in alreadyNotified && it.timestamp <= now + 10 * 60_000 }
            .filter { alertKeywords.any { kw -> it.title.contains(kw, ignoreCase = true) } || KeywordMatcher.matches(it.title, alertKeywords) }
            .filter { topicKeywords.isEmpty() || KeywordMatcher.matches(it.title, topicKeywords) }
            .distinctBy { NewsFilters.headlineKey(it.title) }
            .sortedByDescending { it.timestamp }
            .toList()
    }
}
