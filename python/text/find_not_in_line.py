#!/usr/bin/env python3
# coding=utf-8

"""
transplanted from kotlin script
find lines which are not contained in a bigfile that you specified

chmod +x find_not_in_line.py
./find_not_in_line.py
"""

import sys


def find_not_in_line(super_set, source_set, target_set):
    with open(super_set, "rt") as f:
        super_s = f.readlines()
    with open(source_set, "rt") as f:
        sourse_s = f.readlines()
    with open(target_set, "wt") as f:
        for line in super_s:
            if line not in sourse_s:
                f.write(line)


if __name__ == '__main__':
    args = sys.argv

    if len(args) != 4:
        print(f"Usage {args[0]} super_set source_set target_set")
        sys.exit(1)

    find_not_in_line(*args[1:4])
