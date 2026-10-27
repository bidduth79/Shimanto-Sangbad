import os

# Fix MainActivity.kt
filepath = "app/src/main/java/com/example/MainActivity.kt"
with open(filepath, "r") as f:
    content = f.read()

old_share_text = 'val shareText = "${article.title}\\n\\n$finalUrl"'
new_share_text = 'val shareText = finalUrl'
content = content.replace(old_share_text, new_share_text)

with open(filepath, "w") as f:
    f.write(content)

# Fix VideoList.kt
filepath = "app/src/main/java/com/example/ui/VideoList.kt"
with open(filepath, "r") as f:
    content = f.read()

old_extra = 'putExtra(android.content.Intent.EXTRA_TEXT, "$title\\n\\nhttps://www.youtube.com/watch?v=$videoId")'
new_extra = 'putExtra(android.content.Intent.EXTRA_TEXT, "https://www.youtube.com/watch?v=$videoId")'
content = content.replace(old_extra, new_extra)

with open(filepath, "w") as f:
    f.write(content)

