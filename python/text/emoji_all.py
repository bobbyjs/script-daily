from typing import List

import requests
from bs4 import BeautifulSoup

"""
pip install BeautifulSoup4

数据来源：https://www.emojiall.com

https://github.com/vdurmont/emoji-java/issues/169

"""

emoji_all_url = "https://www.emojiall.com/zh-hans/all-emojis"


class Emoji:
    value: str
    chinese_name: str


def fetch_html() -> str:
    # with open("emoji_all.html", "r") as f:
    #     return f.read()
    resp = requests.get(emoji_all_url)
    if resp.status_code != 200:
        raise Exception(f"failed to GET {emoji_all_url}: "
                        f"status_code={resp.status_code}, resp={resp.text}")
    return resp.text


def parse_emoji(html_doc: str) -> List[Emoji]:
    emojis = []
    soup = BeautifulSoup(html_doc, 'html.parser')
    cards = soup.find_all(class_="emoji_card")
    for card in cards:
        emoji_font = card.find(class_="emoji_font")
        if not emoji_font:
            continue
        emoji = Emoji()
        emoji.value = emoji_font.text
        emojis.append(emoji)
        emoji_name = card.find(class_="emoji_name")
        if emoji_name and emoji_name.text:
            emoji.chinese_name = emoji_name.text.strip()
    return emojis


if __name__ == '__main__':
    html = fetch_html()
    emoji_list = parse_emoji(html)
    with open("emoji_all.txt", "w") as f1, \
            open("emoji_all_with_desc.txt", "w") as f2:
        for emoji in emoji_list:
            f1.write(f"{emoji.value}\n")
            f2.write(f"{emoji.value} {emoji.chinese_name}\n")
