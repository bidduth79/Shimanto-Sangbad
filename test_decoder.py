import urllib.request
import urllib.parse
import json
import re

def decode_google_news_url(url):
    try:
        match = re.search(r'articles/([^?]+)', url)
        if not match:
            return None
        article_id = match.group(1)
        
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'})
        html = urllib.request.urlopen(req).read().decode('utf-8', errors='ignore')
        
        sig_match = re.search(r'data-n-a-sg="([^"]+)"', html)
        time_match = re.search(r'data-n-a-ts="([^"]+)"', html)
        
        if not sig_match or not time_match:
            return None
            
        sig = sig_match.group(1)
        ts = time_match.group(1)
        
        payload = f'[[["Fbv4je","[\\"news.google.com/news/rss/articles/{article_id}?oc=5\\",None,\\"{sig}\\",\\"{ts}\\",None,\\"\\",\\"\\"]",null,"generic"]]]'
        data = urllib.parse.urlencode({'f.req': payload}).encode('utf-8')
        batch_url = 'https://news.google.com/_/DotsSplashUi/data/batchexecute'
        req = urllib.request.Request(batch_url, data=data, headers={
            'Content-Type': 'application/x-www-form-urlencoded;charset=utf-8',
            'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
        })
        resp = urllib.request.urlopen(req).read().decode('utf-8')
        print("Response:")
        print(resp)
    except Exception as e:
        print("Error:", e)

decode_google_news_url('https://news.google.com/rss/articles/CBMiXEFVX3lxTE5DMmZIRVdNX0N3OUFDWll3RFJhM1VfclFVbTEwZ0xjb1lqOHB6dlY2bjFJUUVuVWdCMDBZaFFCLWZyUVg0akpBeTVFRVBwTkF2RjhQMnVsZmF6bTBG0gFqQVVfeXFMTXdiUDNfRGJ6NENhNWZ2b3NVbVRPUHBMYU53UlBjUFBKd01MUHQyYWFmWXplbUhiTTNEVU9SSi1Vc2JoS1gtSW1XQnhPNlk1bk5SMFVpWUg1S3BMaEdxVVlmaU96eFdhdkNHZw?oc=5')
