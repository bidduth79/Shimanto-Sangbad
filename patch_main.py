import os

filepath = "app/src/main/java/com/example/MainActivity.kt"
with open(filepath, "r") as f:
    content = f.read()

# Add schedule call in onCreate
if "import com.example.data.broadcast.NewsBroadcastScheduler" not in content:
    content = content.replace("import android.os.Bundle", "import android.os.Bundle\nimport com.example.data.broadcast.NewsBroadcastScheduler")

if "NewsBroadcastScheduler.scheduleNext" not in content:
    content = content.replace("setContent {", "NewsBroadcastScheduler.scheduleNext(this)\n        setContent {")

# Update SettingsContent call
old_settings = """                com.example.ui.SettingsContent(
                  keywords = uiState.keywords,
                  youtubeApiKey = uiState.youtubeApiKey,
                  youtubeChannelId = uiState.youtubeChannelId,
                  onAddKeyword = { viewModel.addKeyword(it) },
                  onRemoveKeyword = { viewModel.removeKeyword(it) },
                  onSaveApiKey = { viewModel.updateYoutubeApiKey(it) },
                  onSaveChannelId = { viewModel.updateYoutubeChannelId(it) },
                  modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                )"""
new_settings = """                com.example.ui.SettingsContent(
                  keywords = uiState.keywords,
                  youtubeApiKey = uiState.youtubeApiKey,
                  youtubeChannelId = uiState.youtubeChannelId,
                  onAddKeyword = { viewModel.addKeyword(it) },
                  onRemoveKeyword = { viewModel.removeKeyword(it) },
                  onSaveApiKey = { viewModel.updateYoutubeApiKey(it) },
                  onSaveChannelId = { viewModel.updateYoutubeChannelId(it) },
                  telegramChatId = uiState.telegramChatId,
                  onSaveTelegramChatId = { viewModel.updateTelegramChatId(it) },
                  modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                )"""
content = content.replace(old_settings, new_settings)

with open(filepath, "w") as f:
    f.write(content)

