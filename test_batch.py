import urllib.request
import urllib.parse
import json
import re

url = "https://news.google.com/rss/articles/CBMiXEFVX3lxTE5DMmZIRVdNX0N3OUFDWll3RFJhM1VfclFVbTEwZ0xjb1lqOHB6dlY2bjFJUUVuVWdCMDBZaFFCLWZyUVg0akpBeTVFRVBwTkF2RjhQMnVsZmF6bTBG0gFqQVVfeXFMTXdiUDNfRGJ6NENhNWZ2b3NVbVRPUHBMYU53UlBjUFBKd01MUHQyYWFmWXplbUhiTTNEVU9SSi1Vc2JoS1gtSW1XQnhPNlk1bk5SMFVpWUg1S3BMaEdxVVlmaU96eFdhdkNHZw?oc=5"
article_id = url.split('articles/')[1].split('?')[0]

req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'})
html = urllib.request.urlopen(req).read().decode('utf-8', errors='ignore')

sig = re.search(r'data-n-a-sg="([^"]+)"', html).group(1)
ts = re.search(r'data-n-a-ts="([^"]+)"', html).group(1)

inner_arr = [
    "garturlreq",
    [["X","X",["X","X"],None,None,1,1,"US:en",None,1,None,None,None,None,None,0,1],"X","X",1,[1,1,1],1,1,None,0,0,None,0],
    article_id,
    int(ts),
    sig
]
inner_payload = json.dumps(inner_arr)
payload = json.dumps([[["Fbv4je", inner_payload]]])

data = urllib.parse.urlencode({'f.req': payload}).encode('utf-8')
batch_url = 'https://news.google.com/_/DotsSplashUi/data/batchexecute'
req = urllib.request.Request(batch_url, data=data, headers={
    'Content-Type': 'application/x-www-form-urlencoded;charset=utf-8',
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'
})
try:
    resp = urllib.request.urlopen(req).read().decode('utf-8')
    print("Success!")
    print(resp[:500])
except Exception as e:
    print("Error:", e)
