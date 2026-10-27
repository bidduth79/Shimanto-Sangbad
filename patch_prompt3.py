import os

filepath = "app/src/main/java/com/example/data/broadcast/TelegramBroadcastWorker.kt"
with open(filepath, "r") as f:
    content = f.read()

old_msg = """                    <b>শ্রদ্ধেয় জেনারেল</b>
                    <b>আসসালামু আলাইকুম স্যার,</b>

                    <b>নিউজ আপডেট</b>
                    <b>$currentDateStr</b> 

                    $generatedText"""

new_msg = """                    <b>শ্রদ্ধেয় জেনারেল</b>
                    <b>আসসালামু আলাইকুম স্যার,</b>

                    <b>নিউজ আপডেট</b>
                    <b>$currentDateStr</b> 

                    $generatedText"""

# Wait, let's just use HTML for everything. Let me write a script that correctly sets it up.
