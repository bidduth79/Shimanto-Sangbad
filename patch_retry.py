import os

filepath = "app/src/main/java/com/example/data/broadcast/TelegramBroadcastWorker.kt"
with open(filepath, "r") as f:
    text = f.read()

old_catch = """        } catch (e: retrofit2.HttpException) {
            Log.e("TelegramBroadcastWorker", "HTTP Error in broadcast: ${e.code()} ${e.message()}", e)
            if (e.code() in 500..599 || e.code() == 429) {"""

new_catch = """        } catch (e: retrofit2.HttpException) {
            val url = e.response()?.raw()?.request?.url?.toString() ?: "Unknown URL"
            Log.e("TelegramBroadcastWorker", "HTTP Error in broadcast: ${e.code()} ${e.message()} from $url", e)
            if (e.code() in 500..599 || e.code() == 429) {"""

text = text.replace(old_catch, new_catch)

with open(filepath, "w") as f:
    f.write(text)
