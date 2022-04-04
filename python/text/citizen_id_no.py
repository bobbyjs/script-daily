#!/usr/bin/env python3
# coding=utf-8

"""
./citizen_id_no.py ; echo $?
"""
import sys

coe = "7 9 10 5 8 4 2 1 6 3 7 9 10 5 8 4 2".split(" ")
coe = list(map(lambda x: int(x), coe))

seq = "0 1 2 3 4 5 6 7 8 9 10".split(" ")
res = "1 0 X 9 8 7 6 5 4 3 2".split(" ")
rem_map = dict(zip(seq, res))


def main(no: str):
    if len(no) != 18:
        print("invalid size as a Citizen ID Number.", file=sys.stderr)
        exit(3)
    ns = list(no)
    s = 0
    for k, v in zip(ns[0:17], coe):
        s += int(k) * v
    rem = rem_map[str(s % 11)]
    if rem == ns[-1]:
        print(no)
    else:
        print(f"{no[0:17]}{rem}")
        exit(1)


if __name__ == '__main__':
    args = sys.argv
    if len(args) < 2:
        print("please pass a Citizen ID Number.", file=sys.stderr)
        exit(2)
    no = args[1]
    main(no.strip())
