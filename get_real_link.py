import urllib.request
import ssl
import xml.etree.ElementTree as ET

ssl._create_default_https_context = ssl._create_unverified_context
url = "https://news.google.com/rss/search?q=BD+when:1d&hl=bn&gl=BD&ceid=BD:bn"
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
try:
    resp = urllib.request.urlopen(req)
    tree = ET.parse(resp)
    root = tree.getroot()
    item = root.find('.//item')
    if item:
        link = item.find('link').text
        print("Real Link:", link)
except Exception as e:
    print(e)
