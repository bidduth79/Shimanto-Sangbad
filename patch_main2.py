import os

filepath = "app/src/main/java/com/example/MainActivity.kt"
with open(filepath, "r") as f:
    content = f.read()

old_call = """                com.example.ui.SettingsContent(
                    keywords = uiState.keywords,
                    youtubeApiKey = uiState.youtubeApiKey,
                    youtubeChannelId = uiState.youtubeChannelId,
                    onAddKeyword = { viewModel.addKeyword(it) },
                    onRemoveKeyword = { viewModel.removeKeyword(it) },
                    onSaveApiKey = { viewModel.updateYoutubeApiKey(it) },
                    onSaveChannelId = { viewModel.updateYoutubeChannelId(it) }
                )"""
                
new_call = """                com.example.ui.SettingsContent(
                    keywords = uiState.keywords,
                    youtubeApiKey = uiState.youtubeApiKey,
                    youtubeChannelId = uiState.youtubeChannelId,
                    onAddKeyword = { viewModel.addKeyword(it) },
                    onRemoveKeyword = { viewModel.removeKeyword(it) },
                    onSaveApiKey = { viewModel.updateYoutubeApiKey(it) },
                    onSaveChannelId = { viewModel.updateYoutubeChannelId(it) },
                    telegramChatId = uiState.telegramChatId,
                    onSaveTelegramChatId = { viewModel.updateTelegramChatId(it) }
                )"""

content = content.replace(old_call, new_call)

with open(filepath, "w") as f:
    f.write(content)
