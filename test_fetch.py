import urllib.request
import json
import base64
import re

url = "https://news.google.com/rss/articles/CBMiXEFVX3lxTE5DMmZIRVdNX0N3OUFDWll3RFJhM1VfclFVbTEwZ0xjb1lqOHB6dlY2bjFJUUVuVWdCMDBZaFFCLWZyUVg0akpBeTVFRVBwTkF2RjhQMnVsZmF6bTBG0gFqQVVfeXFMTXdiUDNfRGJ6NENhNWZ2b3NVbVRPUHBMYU53UlBjUFBKd01MUHQyYWFmWXplbUhiTTNEVU9SSi1Vc2JoS1gtSW1XQnhPNlk1bk5SMFVpWUg1S3BMaEdxVVlmaU96eFdhdkNHZw?oc=5"
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'})
try:
    resp = urllib.request.urlopen(req)
    html = resp.read().decode('utf-8', errors='ignore')
    # search for standard redirect
    print("Length of html:", len(html))
    match = re.search(r'data-n-au="([^"]+)"', html)
    if match:
        print("data-n-au:", match.group(1))
    
    # Check for <c-wiz> data-n-au
    match2 = re.search(r'data-n-au="([^"]+)"', html)
    if match2:
        print("data-n-au2:", match2.group(1))
        
    # extract all URLs that don't belong to google
    urls = re.findall(r'https?://[^\s"\'<>]+', html)
    non_google = [u for u in urls if 'google' not in u and 'gstatic' not in u and 'w3.org' not in u]
    print("Non-google URLs in HTML:", set(non_google))
except Exception as e:
    print(e)

