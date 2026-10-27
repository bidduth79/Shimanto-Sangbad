import os

filepath = "app/src/main/java/com/example/data/broadcast/TelegramBroadcastWorker.kt"
with open(filepath, "r") as f:
    text = f.read()

old_req = """                val tgRequest = TelegramMessageRequest(
                    chatId = chatId,
                    text = finalMessage,
                    parseMode = "HTML",
                    disableWebPagePreview = true
                )
                tgApi.sendMessage(botToken, tgRequest)"""

new_req = """                val tgRequest = TelegramMessageRequest(
                    chatId = chatId,
                    text = finalMessage,
                    parseMode = "HTML",
                    disableWebPagePreview = true
                )
                Log.d("TelegramBroadcastWorker", "Sending message to Telegram...")
                tgApi.sendMessage(botToken, tgRequest)
                Log.d("TelegramBroadcastWorker", "Telegram message sent successfully")"""

text = text.replace(old_req, new_req)

with open(filepath, "w") as f:
    f.write(text)
