import os

filepath = "app/src/main/java/com/example/data/broadcast/TelegramBroadcastWorker.kt"
with open(filepath, "r") as f:
    lines = f.readlines()

out = []
skip = False
for line in lines:
    if "val articlesText = articles.mapIndexed {" in line:
        skip = True
        
        new_block = """            val sdfTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale("bn", "BD"))
            sdfTime.timeZone = java.util.TimeZone.getTimeZone("Asia/Dhaka")
            val articlesText = articles.mapIndexed { idx, art -> 
                val timeStr = if (art.timestamp > 0L) sdfTime.format(java.util.Date(art.timestamp)) else "N/A"
                "ID: $idx, Title: ${art.title}, Source: ${art.source}, Time: $timeStr" 
            }.joinToString("\\n")
            
            val prompt = \"\"\"
                You are a news curator. Here is a list of news articles:
                $articlesText
                
                Select up to 10 most important news articles (prioritizing border incidents, BGB/BSF, national security). 
                Return them exactly in this format using Bengali numerals (১, ২, ৩...):
                *১। [Title]। ([Source], [Time])*
                *২। [Title]। ([Source], [Time])*
                
                Make sure the title and source are in Bengali.
                Do not include any other text, intro, or links. Output must strictly be the formatted list.
            \"\"\".trimIndent()
            
            val request = GenerateContentRequest(
                contents = listOf(Content(listOf(Part(text = prompt))))
            )
            
            val response = geminiApi.generateContent(apiKey, request)
            val generatedText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
            
            if (generatedText.isNullOrBlank()) {
                Log.e("TelegramBroadcastWorker", "Empty response from Gemini")
                return@withContext Result.failure()
            }
            
            // 4. Send to Telegram
            val prefs = applicationContext.getSharedPreferences("keywords_prefs", Context.MODE_PRIVATE)
            val botToken = prefs.getString("telegram_bot_token", "8513488470:AAGO9I43FcsNARzx4F6b338jH8eUVTbynCQ") ?: ""
            val chatId = prefs.getString("telegram_chat_id", "6123653565") ?: "6123653565"
            
            if (botToken.isNotBlank() && chatId.isNotBlank()) {
                val tgRetrofit = Retrofit.Builder()
                    .baseUrl("https://api.telegram.org/")
                    .client(okHttpClient)
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .build()
                val tgApi = tgRetrofit.create(TelegramApiService::class.java)
                
                val sdfDate = java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale("bn", "BD"))
                sdfDate.timeZone = java.util.TimeZone.getTimeZone("Asia/Dhaka")
                val currentDateStr = sdfDate.format(java.util.Date())
                
                val finalMessage = \"\"\"
                    *শ্রদ্ধেয় জেনারেল*
                    *আসসালামু আলাইকুম স্যার,*

                    *নিউজ আপডেট*
                    *$currentDateStr* 

                    $generatedText
                \"\"\".trimIndent()
                
                val tgRequest = TelegramMessageRequest(
                    chatId = chatId,
                    text = finalMessage,
                    parseMode = "Markdown"
                )
                tgApi.sendMessage(botToken, tgRequest)
            }
"""
        out.append(new_block)
    elif skip and "tgApi.sendMessage(botToken, tgRequest)" in line:
        skip = False
    elif skip and "}" in line and "return@withContext Result.success()" in lines[lines.index(line)+1]:
        pass # Handle closing brace separately if needed, but easier to just use index bounds
        
with open(filepath, "r") as f:
    text = f.read()

start_str = "            val articlesText ="
end_str = "                tgApi.sendMessage(botToken, tgRequest)\n            }"

start_idx = text.find(start_str)
end_idx = text.find(end_str) + len(end_str)

new_text = text[:start_idx] + new_block + text[end_idx:]

with open(filepath, "w") as f:
    f.write(new_text)

