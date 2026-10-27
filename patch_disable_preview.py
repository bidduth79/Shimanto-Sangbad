import os

filepath = "app/src/main/java/com/example/data/api/TelegramApiModels.kt"
with open(filepath, "r") as f:
    text = f.read()

text = text.replace('val parseMode: String = "HTML"', 'val parseMode: String = "HTML",\n    @Json(name = "disable_web_page_preview") val disableWebPagePreview: Boolean = true')

with open(filepath, "w") as f:
    f.write(text)
