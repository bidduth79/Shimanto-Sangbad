import os

filepath = "app/src/main/java/com/example/data/broadcast/TelegramBroadcastWorker.kt"
with open(filepath, "r") as f:
    text = f.read()

old_req = """            val request = GenerateContentRequest(
                contents = listOf(Content(listOf(Part(text = prompt))))
            )
            
            val response = geminiApi.generateContent(apiKey, request)"""

new_req = """            val request = GenerateContentRequest(
                contents = listOf(Content(listOf(Part(text = prompt))))
            )
            
            Log.d("TelegramBroadcastWorker", "Sending request to Gemini API...")
            val response = geminiApi.generateContent(apiKey, request)
            Log.d("TelegramBroadcastWorker", "Received response from Gemini API")"""

text = text.replace(old_req, new_req)

with open(filepath, "w") as f:
    f.write(text)
