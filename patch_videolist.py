import os

filepath = "app/src/main/java/com/example/ui/VideoList.kt"
with open(filepath, "r") as f:
    content = f.read()

# For the featured video (first item)
old_featured = """                    IconButton(
                        onClick = {
                            if (videoId.isNotBlank()) {
                                clipboardManager.setText(AnnotatedString("https://www.youtube.com/watch?v=$videoId"))
                                Toast.makeText(context, "লিংক কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "লিংক কপি করুন",
                            tint = Primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }"""

new_featured = """                    IconButton(
                        onClick = {
                            if (videoId.isNotBlank()) {
                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    setPackage("com.whatsapp")
                                    putExtra(android.content.Intent.EXTRA_TEXT, "$title\\n\\nhttps://www.youtube.com/watch?v=$videoId")
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "আপনার ফোনে WhatsApp ইনস্টল করা নেই", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Share to WhatsApp",
                            tint = Color(0xFF25D366),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            if (videoId.isNotBlank()) {
                                clipboardManager.setText(AnnotatedString("https://www.youtube.com/watch?v=$videoId"))
                                Toast.makeText(context, "লিংক কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "লিংক কপি করুন",
                            tint = Primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }"""
content = content.replace(old_featured, new_featured)

old_list_item = """                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString("https://www.youtube.com/watch?v=$videoId"))
                            Toast.makeText(context, "লিংক কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("কপি", fontSize = 12.sp)
                    }"""

new_list_item = """                    OutlinedButton(
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                setPackage("com.whatsapp")
                                putExtra(android.content.Intent.EXTRA_TEXT, "$title\\n\\nhttps://www.youtube.com/watch?v=$videoId")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "WhatsApp ইনস্টল করা নেই", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            tint = Color(0xFF25D366),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString("https://www.youtube.com/watch?v=$videoId"))
                            Toast.makeText(context, "লিংক কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("কপি", fontSize = 12.sp)
                    }"""
content = content.replace(old_list_item, new_list_item)

with open(filepath, "w") as f:
    f.write(content)
