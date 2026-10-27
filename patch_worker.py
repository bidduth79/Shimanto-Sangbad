import os

filepath = "app/src/main/java/com/example/data/broadcast/TelegramBroadcastWorker.kt"
with open(filepath, "r") as f:
    text = f.read()

old_call = """                tgApi.sendMessage(botToken, tgRequest)"""
new_call = """                val url = "https://api.telegram.org/bot$botToken/sendMessage"
                tgApi.sendMessage(url, tgRequest)"""

text = text.replace(old_call, new_call)

with open(filepath, "w") as f:
    f.write(text)
