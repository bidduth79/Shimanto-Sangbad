import urllib.request
import json

url = "https://api.github.com/search/code?q=CBMi+batchexecute+language:python"
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
try:
    resp = urllib.request.urlopen(req)
    data = json.loads(resp.read().decode('utf-8'))
    for item in data.get('items', [])[:3]:
        print(item['html_url'])
except Exception as e:
    print(e)
