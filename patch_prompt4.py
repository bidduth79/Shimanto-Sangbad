import os

filepath = "app/src/main/java/com/example/data/broadcast/TelegramBroadcastWorker.kt"
with open(filepath, "r") as f:
    text = f.read()

# Make sure Gemini outputs with <b>
old_prompt = """                Return them exactly in this format using Bengali numerals (১, ২, ৩...):
                ১। [Title]। ([Source], [Time])
                ২। [Title]। ([Source], [Time])
                
                Make sure the title and source are in Bengali."""
                
new_prompt = """                Return them exactly in this format using Bengali numerals (১, ২, ৩...):
                <b>১। [Title]। ([Source], [Time])</b>
                <b>২। [Title]। ([Source], [Time])</b>
                
                Make sure the title and source are in Bengali."""

text = text.replace(old_prompt, new_prompt)

with open(filepath, "w") as f:
    f.write(text)
