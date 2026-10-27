curl -s "https://news.google.com/rss/search?q=BD+when:1d&hl=bn&gl=BD&ceid=BD:bn" | grep -A 10 "<item>" | head -n 15
