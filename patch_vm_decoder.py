import os

filepath = "app/src/main/java/com/example/ui/NewsViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

old_decoder = """        viewModelScope.launch {
            val articleLink = article.originalLink ?: article.link
            val finalUrl = com.example.data.GoogleNewsDecoder.decodeUrl(articleLink)
            onLinkResolved(finalUrl)
        }"""
        
new_decoder = """        viewModelScope.launch {
            // Since we use direct API now, links are mostly direct, no decoding needed
            val finalUrl = article.originalLink ?: article.link
            onLinkResolved(finalUrl)
        }"""

content = content.replace(old_decoder, new_decoder)

with open(filepath, "w") as f:
    f.write(content)
