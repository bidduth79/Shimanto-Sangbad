import sys

with open('app/src/main/java/com/example/ui/NewsViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace("    }\n}\n\n    private suspend fun fetchVideos() {", "    }\n\n    private suspend fun fetchVideos() {")
content = content.replace("    }\n}\n    private suspend fun fetchVideos() {", "    }\n\n    private suspend fun fetchVideos() {")

if not content.endswith("}"):
    content = content.strip()
    if not content.endswith("}"):
        content += "\n}"

with open('app/src/main/java/com/example/ui/NewsViewModel.kt', 'w') as f:
    f.write(content)
