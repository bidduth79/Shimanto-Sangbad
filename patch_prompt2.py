import os

filepath = "app/src/main/java/com/example/data/broadcast/TelegramBroadcastWorker.kt"
with open(filepath, "r") as f:
    content = f.read()

old_prompt = """                Select up to 10 most important news articles (prioritizing border incidents, BGB/BSF, national security). 
                Return them exactly in this format using Bengali numerals (১, ২, ৩...):
                *১। [Title]। ([Source], [Time])*
                *২। [Title]। ([Source], [Time])*"""

new_prompt = """                Select up to 10 most important news articles (prioritizing border incidents, BGB/BSF, national security). 
                Return them exactly in this format using Bengali numerals (১, ২, ৩...):
                ১। [Title]। ([Source], [Time])
                ২। [Title]। ([Source], [Time])"""

content = content.replace(old_prompt, new_prompt)

old_msg = """                    *শ্রদ্ধেয় জেনারেল*
                    *আসসালামু আলাইকুম স্যার,*

                    *নিউজ আপডেট*
                    *$currentDateStr* 

                    $generatedText"""
                    
new_msg = """                    <b>শ্রদ্ধেয় জেনারেল</b>
                    <b>আসসালামু আলাইকুম স্যার,</b>

                    <b>নিউজ আপডেট</b>
                    <b>$currentDateStr</b> 

                    $generatedText"""

content = content.replace(old_msg, new_msg)
content = content.replace('parseMode = "Markdown"', 'parseMode = "HTML"')

with open(filepath, "w") as f:
    f.write(content)
