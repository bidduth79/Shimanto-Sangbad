import os

filepath = "app/src/main/java/com/example/data/broadcast/TelegramBroadcastWorker.kt"
with open(filepath, "r") as f:
    text = f.read()

text = text.replace('val botToken = prefs.getString("telegram_bot_token", "8513488470:AAGO9I43FcsNARzx4F6b338jH8eUVTbynCQ") ?: ""', 'val botToken = (prefs.getString("telegram_bot_token", "8513488470:AAGO9I43FcsNARzx4F6b338jH8eUVTbynCQ") ?: "").trim()')
text = text.replace('val chatId = prefs.getString("telegram_chat_id", "6123653565") ?: "6123653565"', 'val chatId = (prefs.getString("telegram_chat_id", "6123653565") ?: "6123653565").trim()')

with open(filepath, "w") as f:
    f.write(text)
