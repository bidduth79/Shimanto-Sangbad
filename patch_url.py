import os

filepath = "app/src/main/java/com/example/data/api/TelegramApiService.kt"
with open(filepath, "r") as f:
    text = f.read()

text = text.replace('@POST("/bot{token}/sendMessage")', '@POST("./bot{token}/sendMessage")')

with open(filepath, "w") as f:
    f.write(text)
