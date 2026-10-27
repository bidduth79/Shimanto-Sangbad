import os

filepath = "app/src/main/java/com/example/data/api/TelegramApiService.kt"
with open(filepath, "r") as f:
    text = f.read()

old_fun = """    @POST("./bot{token}/sendMessage")
    suspend fun sendMessage(
        @Path(value = "token", encoded = true) token: String,
        @Body request: TelegramMessageRequest
    ): TelegramMessageResponse"""

new_fun = """    @POST
    suspend fun sendMessage(
        @retrofit2.http.Url url: String,
        @Body request: TelegramMessageRequest
    ): TelegramMessageResponse"""

text = text.replace("import retrofit2.http.Path", "")
text = text.replace(old_fun, new_fun)

with open(filepath, "w") as f:
    f.write(text)
