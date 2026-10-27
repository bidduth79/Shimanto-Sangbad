import urllib.request
import ssl
import xml.etree.ElementTree as ET

ssl._create_default_https_context = ssl._create_unverified_context
url = "https://news.google.com/rss/search?q=Bangladesh+when:1d&hl=en-US&gl=US&ceid=US:en"
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
try:
    resp = urllib.request.urlopen(req)
    tree = ET.parse(resp)
    root = tree.getroot()
    item = root.find('.//item')
    if item:
        for child in item:
            print(f"{child.tag}: {child.text}")
            if child.attrib:
                print(f"  attribs: {child.attrib}")
except Exception as e:
    print(e)
