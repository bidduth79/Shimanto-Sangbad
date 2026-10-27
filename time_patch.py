import os

filepath = "app/src/main/java/com/example/MainActivity.kt"
with open(filepath, "r") as f:
    content = f.read()

import_str = "import android.text.format.DateUtils"
if import_str not in content:
    content = content.replace("import androidx.compose.runtime.*", "import androidx.compose.runtime.*\n" + import_str)
    
new_time_format_str = """
fun formatRelativeTime(timestamp: Long, fallback: String): String {
    if (timestamp == 0L) return fallback
    return DateUtils.getRelativeTimeSpanString(
        timestamp,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS
    ).toString()
}
"""

if "fun formatRelativeTime" not in content:
    content = content + "\n" + new_time_format_str

# Replace text = article.pubDate with text = formatRelativeTime(article.timestamp, article.pubDate)
content = content.replace("text = article.pubDate", "text = formatRelativeTime(article.timestamp, article.pubDate)")

with open(filepath, "w") as f:
    f.write(content)
