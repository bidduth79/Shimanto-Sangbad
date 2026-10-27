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
        
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'})
        html = urllib.request.urlopen(req).read().decode('utf-8', errors='ignore')
        
        sig_match = re.search(r'data-n-a-sg="([^"]+)"', html)
        time_match = re.search(r'data-n-a-ts="([^"]+)"', html)
        
        if not sig_match or not time_match:
            print("sig/ts not found")
            return None
            
        sig = sig_match.group(1)
        ts = time_match.group(1)
        
        inner_payload = f'["garturlreq",[["X","X",["X","X"],null,null,1,1,"US:en",null,1,null,null,null,null,null,0,1],"X","X",1,[1,1,1],1,1,null,0,0,null,0],"{article_id}",{ts},"{sig}"]'
        payload = json.dumps([[["Fbv4je", inner_payload]]])
        
        data = urllib.parse.urlencode({'f.req': payload}).encode('utf-8')
        batch_url = 'https://news.google.com/_/DotsSplashUi/data/batchexecute'
        req = urllib.request.Request(batch_url, data=data, headers={
            'Content-Type': 'application/x-www-form-urlencoded;charset=utf-8',
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'
        })
        resp = urllib.request.urlopen(req).read().decode('utf-8')
        
        # parse the batchexecute response
        parts = resp.split('\n\n')
        if len(parts) > 1:
            try:
                parsed1 = json.loads(parts[1])
                parsed2 = json.loads(parsed1[0][2])
                print("Final URL:", parsed2[1])
            except Exception as e:
                print("Could not parse json:", e, parts[1])
        else:
            print("Response:", resp)
            
    except Exception as e:
        print("Error:", e)

decode_google_news_url('https://news.google.com/rss/articles/CBMiXEFVX3lxTE5DMmZIRVdNX0N3OUFDWll3RFJhM1VfclFVbTEwZ0xjb1lqOHB6dlY2bjFJUUVuVWdCMDBZaFFCLWZyUVg0akpBeTVFRVBwTkF2RjhQMnVsZmF6bTBG0gFqQVVfeXFMTXdiUDNfRGJ6NENhNWZ2b3NVbVRPUHBMYU53UlBjUFBKd01MUHQyYWFmWXplbUhiTTNEVU9SSi1Vc2JoS1gtSW1XQnhPNlk1bk5SMFVpWUg1S3BMaEdxVVlmaU96eFdhdkNHZw?oc=5')
