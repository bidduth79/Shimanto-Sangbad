import os

filepath = "app/src/main/java/com/example/ui/SettingsContent.kt"
with open(filepath, "r") as f:
    content = f.read()

# Add params
old_params = """    onSaveApiKey: (String) -> Unit,
    onSaveChannelId: (String) -> Unit = {},
    modifier: Modifier = Modifier"""
new_params = """    onSaveApiKey: (String) -> Unit,
    onSaveChannelId: (String) -> Unit = {},
    telegramChatId: String = "",
    onSaveTelegramChatId: (String) -> Unit = {},
    modifier: Modifier = Modifier"""
content = content.replace(old_params, new_params)

# Add state
old_state = "    var showAddKeywordDialog by remember { mutableStateOf(false) }"
new_state = """    var showAddKeywordDialog by remember { mutableStateOf(false) }
    var showTelegramDialog by remember { mutableStateOf(false) }"""
content = content.replace(old_state, new_state)

# Add UI card
old_card = """            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
                onClick = { showYoutubeDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = Primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "ইউটিউব এপিআই ও চ্যানেল আইডি",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnBackground
                        )
                        Text(
                            text = if (youtubeApiKey.isNotBlank()) "এপিআই কি সেট করা আছে" else "সেট করা নেই",
                            fontSize = 12.sp,
                            color = Outline
                        )
                    }
                }
            }"""

new_card = old_card + """
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
                onClick = { showTelegramDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "টেলিগ্রাম সেটিংস (Chat ID)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnBackground
                        )
                        Text(
                            text = if (telegramChatId.isNotBlank()) "Chat ID সেট করা আছে" else "সেট করা নেই",
                            fontSize = 12.sp,
                            color = Outline
                        )
                    }
                }
            }"""
content = content.replace(old_card, new_card)

# Add dialog
dialog_code = """
    if (showTelegramDialog) {
        var tempChatId by remember { mutableStateOf(telegramChatId) }
        AlertDialog(
            onDismissRequest = { showTelegramDialog = false },
            title = { Text("টেলিগ্রাম সেটিংস") },
            text = {
                Column {
                    Text("যে চ্যানেল বা গ্রুপে মেসেজ যাবে তার Chat ID দিন (যেমন: @mychannel বা -100123456789)")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempChatId,
                        onValueChange = { tempChatId = it },
                        label = { Text("Chat ID") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onSaveTelegramChatId(tempChatId)
                    showTelegramDialog = false
                }) {
                    Text("সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTelegramDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }"""
content = content.replace("    if (showYoutubeDialog) {", dialog_code + "\n    if (showYoutubeDialog) {")

with open(filepath, "w") as f:
    f.write(content)

