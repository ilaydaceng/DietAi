import urllib.request
import json
import re

def search_dergipark(query):
    query = urllib.parse.quote(query)
    url = f"https://dergipark.org.tr/tr/search?q={query}"
    
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    try:
        html = urllib.request.urlopen(req).read().decode('utf-8')
        
        # Regex to find article links and titles
        pattern = r'<h5 class="card-title">\s*<a href="(https://dergipark\.org\.tr/tr/pub/[^/]+/issue/\d+/\d+)">(.*?)</a>\s*</h5>'
        matches = re.findall(pattern, html, re.DOTALL)
        
        for i, match in enumerate(matches[:3]):
            link = match[0]
            title = match[1].strip()
            print(f"Başlık: {title}")
            print(f"Link: {link}")
            print("-" * 50)
    except Exception as e:
        print("Error:", e)

print("Arama 1: Yapay Zeka Beslenme")
search_dergipark("yapay zeka beslenme")

print("Arama 2: Mobil Uygulama Diyet")
search_dergipark("mobil uygulama diyet")

print("Arama 3: Mobil Sağlık Uygulamaları")
search_dergipark("mobil sağlık uygulamaları")
