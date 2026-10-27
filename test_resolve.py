import urllib.request
import ssl
import re

ssl._create_default_https_context = ssl._create_unverified_context
url = 'https://news.google.com/rss/articles/CBMiXEFVX3lxTE5DMmZIRVdNX0N3OUFDWll3RFJhM1VfclFVbTEwZ0xjb1lqOHB6dlY2bjFJUUVuVWdCMDBZaFFCLWZyUVg0akpBeTVFRVBwTkF2RjhQMnVsZmF6bTBG0gFqQVVfeXFMTXdiUDNfRGJ6NENhNWZ2b3NVbVRPUHBMYU53UlBjUFBKd01MUHQyYWFmWXplbUhiTTNEVU9SSi1Vc2JoS1gtSW1XQnhPNlk1bk5SMFVpWUg1S3BMaEdxVVlmaU96eFdhdkNHZw?oc=5'
req = urllib.request.Request(url)
req.add_header('User-Agent', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)')
try:
    resp = urllib.request.urlopen(req)
    html = resp.read().decode('utf-8', errors='ignore')
    
    match = re.search(r'data-n-au="([^"]+)"', html)
    if match:
        print("data-n-au:", match.group(1))
    else:
        print("data-n-au not found.")
        
    match2 = re.search(r'<c-wiz[^>]*data-n-au="([^"]+)"', html)
    if match2:
        print("c-wiz data-n-au:", match2.group(1))

    # find any url looking like the original publisher
    matches = re.findall(r'https?://(?:(?!google)[^"\'\s])+', html)
    for m in set(matches):
        print("Found:", m)
        
except Exception as e:
    print("Error:", e)
