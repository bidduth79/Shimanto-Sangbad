import os

filepath = "app/src/main/java/com/example/data/broadcast/TelegramBroadcastWorker.kt"
with open(filepath, "r") as f:
    text = f.read()

text = text.replace('val apiKey = BuildConfig.GEMINI_API_KEY', 'val apiKey = BuildConfig.GEMINI_API_KEY.trim()')

with open(filepath, "w") as f:
    f.write(text)
