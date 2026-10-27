import os

filepath = "app/src/main/java/com/example/data/FirebaseManager.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace(
    """.document(article.link.replace("/", "_").replace(".", "_"))""",
    """.document(android.util.Base64.encodeToString(article.link.toByteArray(), android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP))"""
)

content = content.replace("// Log.e(", "Log.e(")

with open(filepath, "w") as f:
    f.write(content)
