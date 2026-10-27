import os

filepath = "app/src/main/java/com/example/data/KeywordPreferences.kt"
with open(filepath, "r") as f:
    content = f.read()

new_methods = """
    fun getTelegramChatId(): String {
        return prefs.getString("telegram_chat_id", "") ?: ""
    }
    
    fun saveTelegramChatId(chatId: String) {
        prefs.edit().putString("telegram_chat_id", chatId).apply()
    }

    fun getYoutubeApiKey(): String {"""

content = content.replace('    fun getYoutubeApiKey(): String {', new_methods)

with open(filepath, "w") as f:
    f.write(content)
