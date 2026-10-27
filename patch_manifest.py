import os

filepath = "app/src/main/AndroidManifest.xml"
with open(filepath, "r") as f:
    content = f.read()

permissions = """    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
    <uses-permission android:name="android.permission.USE_EXACT_ALARM" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />"""

content = content.replace('    <uses-permission android:name="android.permission.INTERNET" />', permissions)

receiver = """        <receiver android:name=".data.broadcast.NewsBroadcastReceiver" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
            </intent-filter>
        </receiver>
    </application>"""

content = content.replace('    </application>', receiver)

with open(filepath, "w") as f:
    f.write(content)
