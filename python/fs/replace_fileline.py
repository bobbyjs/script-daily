#!/usr/bin/env python3
# coding=utf-8

"""
transplanted from kotlin script
/// Create by tuke on 2020/4/23

replace fileline via regexp on dir tree
"""
import argparse
import os
import os.path
import re
import sys


class Main:
    count_line: int = 0

    def __init__(self, args: argparse.Namespace):
        # self.__dict__.update(args.__dict__)
        self.effect: bool = args.effect
        self.infile: str = args.infile
        self.outfile: str = args.outfile
        self.pattern: re.compile = re.compile(args.pattern)
        self.replacement: str = args.replacement
        self.write_matched_only: bool = args.write_matched_only

    def run(self):
        if not os.path.isfile(self.infile):
            print(f"not a file: {self.infile}", file=sys.stderr)
            return
        if os.path.exists(self.outfile):
            print(f"not a directory: {self.outfile}", file=sys.stderr)
            return

        with open(self.infile, mode='rt') as r:
            with open(self.outfile, mode='wt') as w:
                lines = r.readlines()
                for line in lines:
                    if not self.pattern.match(line):
                        if self.write_matched_only:
                            continue
                        new_line = line
                    else:
                        new_line = self.pattern.sub(self.replacement, line)
                    w.write(new_line)
                    w.write(os.linesep)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        description='rename file by regexp')
    parser.add_argument('infile', type=str, help='the input file')
    parser.add_argument('outfile', type=str, help='the outfile file')
    parser.add_argument('-e', '--effect', action='store_true',
                        dest='effect', help='preform actually or not')
    parser.add_argument('-p', '--pattern', '--pattern', required=True,
                        dest='pattern', help='regexp pattern')
    parser.add_argument('-r', '--replacement', required=True,
                        dest='regexp replacement',
                        help='target fileline replacement')
    parser.add_argument('--wmo', '--write-matched-only', action='store_true',
                        dest='write_matched_only',
                        help='write matched lines only')
    args = parser.parse_args()
    Main(args).run()
