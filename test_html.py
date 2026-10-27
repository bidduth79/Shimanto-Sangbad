import urllib.request
import re

url = "https://news.google.com/rss/articles/CBMiXEFVX3lxTE5DMmZIRVdNX0N3OUFDWll3RFJhM1VfclFVbTEwZ0xjb1lqOHB6dlY2bjFJUUVuVWdCMDBZaFFCLWZyUVg0akpBeTVFRVBwTkF2RjhQMnVsZmF6bTBG0gFqQVVfeXFMTXdiUDNfRGJ6NENhNWZ2b3NVbVRPUHBMYU53UlBjUFBKd01MUHQyYWFmWXplbUhiTTNEVU9SSi1Vc2JoS1gtSW1XQnhPNlk1bk5SMFVpWUg1S3BMaEdxVVlmaU96eFdhdkNHZw?oc=5"
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'})
html = urllib.request.urlopen(req).read().decode('utf-8', errors='ignore')

print("bdcom" in html)
print("bdcom.com" in html)

matches = re.findall(r'(https?://(?:www\.)?bdcom[^\s"\'<>]+)', html)
print("Matches:", set(matches))
