#!/usr/bin/env python3

import sys
from datetime import datetime

args = sys.argv[1:]

if len(args) == 0:
    dt = datetime.now()
else:
    dt = datetime.strptime(args[0], "%Y-%m-%d")
print(int(dt.timestamp() * 1000))
