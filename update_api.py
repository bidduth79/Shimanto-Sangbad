import os

filepath = "app/src/main/java/com/example/ui/NewsViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

# Add Ktor dependencies to app/build.gradle.kts first to make sure we can use HTTP client
