import os

filepath = "app/src/main/java/com/example/data/broadcast/TelegramBroadcastWorker.kt"
with open(filepath, "r") as f:
    text = f.read()

old_articles_text = """            val articlesText = articles.mapIndexed { idx, art -> 
                val timeStr = if (art.timestamp > 0L) sdfTime.format(java.util.Date(art.timestamp)) else "N/A"
                "ID: $idx, Title: ${art.title}, Source: ${art.source}, Time: $timeStr" 
            }.joinToString("\\n")"""

new_articles_text = """            val articlesText = articles.mapIndexed { idx, art -> 
                val timeStr = if (art.timestamp > 0L) sdfTime.format(java.util.Date(art.timestamp)) else "N/A"
                "ID: $idx, Title: ${art.title}, Link: ${art.link}, Source: ${art.source}, Time: $timeStr" 
            }.joinToString("\\n")"""

text = text.replace(old_articles_text, new_articles_text)

old_prompt = """                Return them exactly in this format using Bengali numerals (১, ২, ৩...):
                <b>১। [Title]। ([Source], [Time])</b>
                <b>২। [Title]। ([Source], [Time])</b>
                
                Make sure the title and source are in Bengali."""
                
new_prompt = """                Return them exactly in this format using Bengali numerals (১, ২, ৩...), embedding the link into the title using standard HTML anchor tags:
                <b>১। <a href="[Link]">[Title]</a>। ([Source], [Time])</b>
                <b>২। <a href="[Link]">[Title]</a>। ([Source], [Time])</b>
                
                Make sure the title and source are in Bengali. Ensure the HTML tags are perfectly well-formed.
                Use exactly this HTML format: <a href="...">Title</a>"""

text = text.replace(old_prompt, new_prompt)

with open(filepath, "w") as f:
    f.write(text)
