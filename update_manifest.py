import os

filepath = "app/src/main/AndroidManifest.xml"
with open(filepath, "r") as f:
    content = f.read()

if "android.permission.INTERNET" not in content:
    content = content.replace("<application", "<uses-permission android:name=\"android.permission.INTERNET\" />\n    <application")

with open(filepath, "w") as f:
    f.write(content)
