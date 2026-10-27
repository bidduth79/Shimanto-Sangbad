import os

filepath = "app/src/main/java/com/example/ui/VideoList.kt"
with open(filepath, "r") as f:
    content = f.read()

if "import androidx.compose.material.icons.filled.Send" not in content:
    content = content.replace("import androidx.compose.material.icons.filled.PlayArrow", "import androidx.compose.material.icons.filled.PlayArrow\nimport androidx.compose.material.icons.filled.Send\nimport androidx.compose.ui.graphics.Color")

with open(filepath, "w") as f:
    f.write(content)
