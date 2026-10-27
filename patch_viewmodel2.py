import sys

with open('app/src/main/java/com/example/ui/NewsViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace("}\n    private suspend fun fetchVideos() {", "    private suspend fun fetchVideos() {")
content += "\n}"

with open('app/src/main/java/com/example/ui/NewsViewModel.kt', 'w') as f:
    f.write(content)
