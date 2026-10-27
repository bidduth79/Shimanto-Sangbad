import os

filepath = "app/src/main/AndroidManifest.xml"
with open(filepath, "r") as f:
    content = f.read()

if "android:windowSoftInputMode" not in content:
    content = content.replace('android:theme="@style/Theme.MyApplication">', 'android:theme="@style/Theme.MyApplication" android:windowSoftInputMode="adjustResize">')

with open(filepath, "w") as f:
    f.write(content)
