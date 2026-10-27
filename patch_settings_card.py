import os

filepath = "app/src/main/java/com/example/ui/SettingsContent.kt"
with open(filepath, "r") as f:
    content = f.read()

telegram_state = """    var showYouTubeDialog by remember { mutableStateOf(false) }
    var showTelegramDialog by remember { mutableStateOf(false) }"""
content = content.replace("    var showYouTubeDialog by remember { mutableStateOf(false) }", telegram_state)

telegram_dialog = """
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
content = content.replace("    if (showYouTubeDialog) {", telegram_dialog + "\n    if (showYouTubeDialog) {")

telegram_card = """        // --- Telegram Settings Section ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
            onClick = { showTelegramDialog = true }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = Primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "টেলিগ্রাম সেটিংস",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OnBackground
                            )
                            Text(
                                text = if (telegramChatId.isNotBlank()) "Chat ID সেট করা আছে" else "Chat ID সেট করা নেই",
                                fontSize = 12.sp,
                                color = if (telegramChatId.isNotBlank()) Primary else Outline
                            )
                        }
                    }
                    Button(
                        onClick = { showTelegramDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Text("সেটিংস", fontSize = 13.sp)
                    }
                }
            }
        }
"""
content = content.replace("        // --- Keywords Section ---", telegram_card + "\n        // --- Keywords Section ---")

with open(filepath, "w") as f:
    f.write(content)

