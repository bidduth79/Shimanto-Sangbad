import re

filepath = "app/src/main/java/com/example/MainActivity.kt"
with open(filepath, "r") as f:
    content = f.read()

# 1. Add onWhatsAppClick to NewsListWithRefresh signature
content = content.replace(
    "onCopyClick: (NewsArticle) -> Unit",
    "onCopyClick: (NewsArticle) -> Unit,\n    onWhatsAppClick: (NewsArticle) -> Unit"
)

# 2. Add onWhatsAppClick to BgbSpecialNewsCard usage in NewsListWithRefresh
content = content.replace(
    "BgbSpecialNewsCard(article, isVisited, keywords, onArticleClick, onCopyClick)",
    "BgbSpecialNewsCard(article, isVisited, keywords, onArticleClick, onCopyClick, onWhatsAppClick)"
)

# 3. Add onWhatsAppClick to RegularNewsCard usage in NewsListWithRefresh
content = content.replace(
    "RegularNewsCard(article, isVisited, keywords, onArticleClick, onCopyClick)",
    "RegularNewsCard(article, isVisited, keywords, onArticleClick, onCopyClick, onWhatsAppClick)"
)

# 4. Modify BgbSpecialNewsCard Row containing IconButton
bgb_row_old = """        IconButton(
          onClick = {
            onCopyClick(article)
          },
          modifier = Modifier.size(24.dp)
        ) {
          Icon(Icons.Default.ContentCopy, contentDescription = "Copy Link", tint = OnBackground, modifier = Modifier.size(16.dp))
        }"""
bgb_row_new = """        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
          IconButton(
            onClick = { onWhatsAppClick(article) },
            modifier = Modifier.size(24.dp)
          ) {
            Icon(Icons.Default.Send, contentDescription = "Share to WhatsApp", tint = Color(0xFF25D366), modifier = Modifier.size(16.dp))
          }
          IconButton(
            onClick = { onCopyClick(article) },
            modifier = Modifier.size(24.dp)
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Link", tint = OnBackground, modifier = Modifier.size(16.dp))
          }
        }"""
content = content.replace(bgb_row_old, bgb_row_new)

# 5. Modify RegularNewsCard Row containing IconButton
reg_row_old = """        IconButton(
            onClick = {
              onCopyClick(article)
            },
            modifier = Modifier.size(24.dp)
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Link", tint = OnBackground, modifier = Modifier.size(16.dp))
          }"""
reg_row_new = """        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            IconButton(
              onClick = { onWhatsAppClick(article) },
              modifier = Modifier.size(24.dp)
            ) {
              Icon(Icons.Default.Send, contentDescription = "Share to WhatsApp", tint = Color(0xFF25D366), modifier = Modifier.size(16.dp))
            }
            IconButton(
              onClick = { onCopyClick(article) },
              modifier = Modifier.size(24.dp)
            ) {
              Icon(Icons.Default.ContentCopy, contentDescription = "Copy Link", tint = OnBackground, modifier = Modifier.size(16.dp))
            }
          }"""
content = content.replace(reg_row_old, reg_row_new)

# 6. Add implementation to NewsListWithRefresh calls (two of them: BGB and National)
call_old = """                      onCopyClick = { article ->
                          Toast.makeText(this@MainActivity, "মূল লিংক খোঁজা হচ্ছে, একটু অপেক্ষা করুন...", Toast.LENGTH_SHORT).show()
                          viewModel.resolveAndCopyLink(article) { finalUrl ->
                              val clipboard = this@MainActivity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                              val copyText = finalUrl
                              val clip = ClipData.newPlainText("News Link", copyText)
                              clipboard.setPrimaryClip(clip)
                              Toast.makeText(this@MainActivity, "খবরের লিংক কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                          }
                      }
                    )"""
call_new = """                      onCopyClick = { article ->
                          Toast.makeText(this@MainActivity, "মূল লিংক খোঁজা হচ্ছে, একটু অপেক্ষা করুন...", Toast.LENGTH_SHORT).show()
                          viewModel.resolveAndCopyLink(article) { finalUrl ->
                              val clipboard = this@MainActivity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                              val clip = ClipData.newPlainText("News Link", finalUrl)
                              clipboard.setPrimaryClip(clip)
                              Toast.makeText(this@MainActivity, "খবরের লিংক কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                          }
                      },
                      onWhatsAppClick = { article ->
                          Toast.makeText(this@MainActivity, "WhatsApp-এ পাঠানো হচ্ছে...", Toast.LENGTH_SHORT).show()
                          viewModel.resolveAndCopyLink(article) { finalUrl ->
                              val shareText = "${article.title}\\n\\n$finalUrl"
                              val intent = Intent(Intent.ACTION_SEND).apply {
                                  type = "text/plain"
                                  setPackage("com.whatsapp")
                                  putExtra(Intent.EXTRA_TEXT, shareText)
                              }
                              try {
                                  this@MainActivity.startActivity(intent)
                              } catch (e: Exception) {
                                  Toast.makeText(this@MainActivity, "আপনার ফোনে WhatsApp ইনস্টল করা নেই", Toast.LENGTH_SHORT).show()
                              }
                          }
                      }
                    )"""
content = content.replace(call_old, call_new)

with open(filepath, "w") as f:
    f.write(content)
