#!/usr/bin/env python3
# coding=utf-8

"""
transplanted from kotlin script
find lines which are contained in a bigfile that you specified

chmod +x find_in_line.py
./find_in_line.py
"""

import sys


def find_in_line(super_set, source_set, target_set):
    with open(super_set, "rt") as f:
        super_s = f.readlines()
    with open(source_set, "rt") as f:
        sourse_s = f.readlines()
    with open(target_set, "rt") as f:
        for line in sourse_s:
            if line in super_s:
                f.write(line)


if __name__ == '__main__':
    args = sys.argv

    if len(args) != 4:
        print(f"Usage {args[0]} super_set source_set target_set")
        sys.exit(1)

    find_in_line(*args[1:4])
