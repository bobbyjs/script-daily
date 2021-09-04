#!/usr/bin/env python3

import os.path
import re
import requests
import sys
from bs4 import BeautifulSoup

template = """import React from "react";

export default function %s() {
    return (
        %s
            %s
        %s
    );
}
"""


def generate(path, format):
    if not format:
        format = lambda name: "{}Icon".format(name)
    res = requests.get("https://icons.getbootstrap.com/")
    code = res.status_code
    if code != 200:
        sys.stderr.write("status code {}".format(code))
        sys.exit(1)

    bs = BeautifulSoup(markup=res.text, features="lxml")
    elems = bs.find_all(name="li", class_="col mb-4")
    for e in elems:
        name = e.find(class_="name text-muted text-center pt-1").text
        m = re.search(" ", name)
        name = name[:m.start()] + name[m.end():].capitalize()
        name = format(name)
        filename = os.path.join(path, "{}.js".format(name))

        svg = e.div.svg
        path = re.sub("></path>", "/>", str(svg.path))
        svg = str(e.div.svg)
        svg_start = svg[:re.search('>', svg).start() + 1]
        svg_end = "</svg>"
        with open(filename, "w") as fp:
            print("write %s svg to %s" % (name, filename))
            print(svg)
            fp.write(template % (name, svg_start, path, svg_end))
        break


if __name__ == '__main__':
    generate("./", None)
