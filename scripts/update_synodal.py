#!/usr/bin/env python3
"""Update synodal translation in bible.sqlite from RussianSynodalBible.xml"""
import sqlite3
try:
    import defusedxml.ElementTree as ET
except ImportError:
    raise SystemExit("Install defusedxml first: pip install defusedxml")

DB_PATH = 'app/src/main/assets/bible.sqlite'
XML_PATH = '/Users/edgarakert/Downloads/RussianSynodalBible.xml'

print("Parsing XML...")
tree = ET.parse(XML_PATH)
root = tree.getroot()

verses = []
for book_el in root.findall('.//book'):
    book_id = int(book_el.get('number'))
    for chapter_el in book_el.findall('chapter'):
        chapter = int(chapter_el.get('number'))
        for verse_el in chapter_el.findall('verse'):
            verse_num = int(verse_el.get('number'))
            text = (verse_el.text or '').strip()
            verses.append((book_id, chapter, verse_num, text))

print(f"Parsed {len(verses)} verses from XML")

conn = sqlite3.connect(DB_PATH)
cur = conn.cursor()

cur.execute("DELETE FROM verses WHERE translation = 'synodal'")
deleted = cur.rowcount
print(f"Deleted {deleted} old synodal verses")

cur.executemany(
    "INSERT INTO verses (translation, book_id, chapter, verse, text) VALUES ('synodal', ?, ?, ?, ?)",
    verses
)
print(f"Inserted {len(verses)} new synodal verses")

conn.commit()

cur.execute("SELECT COUNT(*) FROM verses WHERE translation='synodal'")
count = cur.fetchone()[0]
print(f"Verification — synodal in DB: {count}")

cur.execute("SELECT translation, COUNT(*) FROM verses GROUP BY translation ORDER BY translation")
for row in cur.fetchall():
    print(f"  {row[0]}: {row[1]}")

conn.close()
print("Done!")
