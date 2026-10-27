import os

filepath = "app/src/main/java/com/example/data/broadcast/TelegramBroadcastWorker.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace("chatId = chatId,\\n                    text = finalMessage", "chatId = chatId,\\n                    text = finalMessage,\\n                    parseMode = \\\"Markdown\\\"")

with open(filepath, "w") as f:
    f.write(content)
