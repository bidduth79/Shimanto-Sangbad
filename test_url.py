import base64
import re

url = "https://news.google.com/rss/articles/CBMiXEFVX3lxTE5DMmZIRVdNX0N3OUFDWll3RFJhM1VfclFVbTEwZ0xjb1lqOHB6dlY2bjFJUUVuVWdCMDBZaFFCLWZyUVg0akpBeTVFRVBwTkF2RjhQMnVsZmF6bTBG0gFqQVVfeXFMTXdiUDNfRGJ6NENhNWZ2b3NVbVRPUHBMYU53UlBjUFBKd01MUHQyYWFmWXplbUhiTTNEVU9SSi1Vc2JoS1gtSW1XQnhPNlk1bk5SMFVpWUg1S3BMaEdxVVlmaU96eFdhdkNHZw"
match = re.search(r'articles/([A-Za-z0-9_\-]+)', url)
if match:
    base64Str = match.group(1)
    padded = base64Str + '=' * ((4 - len(base64Str) % 4) % 4)
    decodedBytes = base64.urlsafe_b64decode(padded)
    decodedStr = decodedBytes.decode('utf-8', errors='ignore')
    print("Decoded string:", decodedStr)
    urlMatch = re.search(r'(https?://[\w\d\-_\.]+[\w\-\.,@?^=%&:/~+#]*)', decodedStr)
    if urlMatch:
        print("URL Match:", urlMatch.group(1))
    else:
        print("No match found")
