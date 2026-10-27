import os

filepath = "app/build.gradle.kts"
with open(filepath, "r") as f:
    content = f.read()

old_bc = 'buildConfigField("String", "YOUTUBE_API_KEY", "\\"${project.findProperty("YOUTUBE_API_KEY") ?: ""}\\"")'
new_bc = 'buildConfigField("String", "YOUTUBE_API_KEY", "\\"${project.findProperty("YOUTUBE_API_KEY") ?: ""}\\"")\n        buildConfigField("String", "GEMINI_API_KEY", "\\"${project.findProperty("GEMINI_API_KEY") ?: ""}\\"")'

content = content.replace(old_bc, new_bc)

with open(filepath, "w") as f:
    f.write(content)
