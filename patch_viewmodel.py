import os

filepath = "app/src/main/java/com/example/ui/NewsViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

# Add telegramChatId to uiState
old_state = "    val youtubeChannelId: String = \"\""
new_state = "    val youtubeChannelId: String = \"\",\n    val telegramChatId: String = \"\""
content = content.replace(old_state, new_state)

# Load it in init
old_init = "            _uiState.update { it.copy(youtubeChannelId = keywordPrefs.getYoutubeChannelId()) }"
new_init = "            _uiState.update { it.copy(youtubeChannelId = keywordPrefs.getYoutubeChannelId()) }\n            _uiState.update { it.copy(telegramChatId = keywordPrefs.getTelegramChatId()) }"
content = content.replace(old_init, new_init)

# Add update method
new_method = """
    fun updateTelegramChatId(chatId: String) {
        keywordPrefs.saveTelegramChatId(chatId)
        _uiState.update { it.copy(telegramChatId = chatId) }
    }
"""
content = content.replace("    fun updateYoutubeChannelId(channelId: String) {", new_method + "    fun updateYoutubeChannelId(channelId: String) {")

with open(filepath, "w") as f:
    f.write(content)
