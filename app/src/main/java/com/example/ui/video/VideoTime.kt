package com.example.ui.video

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import com.example.R

// Parsing and Bengali formatting of YouTube publish times.

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

/** "৫ মিনিট আগে • 08 নভেম্বর 2026, 04:28 PM" in Dhaka time, or "" when [publishedAt] is missing. */
fun formatYouTubeDate(publishedAt: String?, resources: android.content.res.Resources): String {
    if (publishedAt.isNullOrBlank()) return ""
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val date = inputFormat.parse(publishedAt.take(19)) ?: return ""
        val timestamp = date.time

        val outputFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.forLanguageTag("bn-BD")).apply {
            timeZone = TimeZone.getTimeZone("Asia/Dhaka")
        }
        val formattedDate = outputFormat.format(date)

        val diff = System.currentTimeMillis() - timestamp
        val minutes = diff / (1000 * 60)
        val hours = minutes / 60
        val days = hours / 24

        val relativeTime = when {
            diff < 0 -> resources.getString(R.string.time_just_now)
            minutes < 60 -> resources.getString(R.string.time_minutes_ago, toBengaliNumber(minutes.toString()))
            hours < 24 -> resources.getString(R.string.time_hours_ago, toBengaliNumber(hours.toString()))
            else -> resources.getString(R.string.time_days_ago, toBengaliNumber(days.toString()))
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
