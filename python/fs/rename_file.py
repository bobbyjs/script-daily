#!/usr/bin/env python3
# coding=utf-8

"""
transplanted from kotlin script
replace filename via regexp on dir tree

# \1, \2 means group N
import re
re.sub('\\w+ (\\w+) = (.+?;)', 'this.\\1 = \\2', 'int a = b.getC();')
"""
import argparse
import os
import os.path
import re
import shutil
import sys


def map_one(one, f):
    if one:
        return f(one)
    else:
        return one


class LimitException(Exception):
    def __init__(self, msg):
        self.msg = msg


class Main:
    count_total: int = 0
    count_file: int = 0
    count_dir: int = 0

    def __init__(self, args: argparse.Namespace):
        # self.__dict__.update(args.__dict__)
        self.effect: bool = args.effect
        self.path: str = args.path
        self.include_dir: bool = args.include_dir
        self.include_file: bool = args.include_file
        self.pattern: re.compile = re.compile(args.pattern)
        self.replacement: str = args.replacement
        self.limit: int = args.limit
        self.content_pattern: re.compile = map_one(
            args.content_pattern, re.compile)
        self.content_replacement: str = args.content_replacement
        self.content_limit: int = args.content_limit

    def run(self):
        path = os.path.abspath(self.path)
        if not os.path.isdir(path):
            print(f"not a directory: {path}", file=sys.stderr)
            return
        try:
            self.handle_dir(path)
        except LimitException as err:
            print(err.msg, file=sys.stderr)
        finally:
            print(f"rename {self.count_file} files, {self.count_dir} dir")

    def handle_dir(self, path: str):
        try:
            files = os.listdir(path)
        except PermissionError:
            print(f"no permission on {path}")
            return
        for name in files:
            f = os.path.join(path, name)
            if os.path.isdir(f):
                self.handle_dir(f)
            elif os.path.isfile(f):
                if self.include_file:
                    self.handle_file(path, name, True)
            else:
                print(f"neither a file or dir: {f}", file=sys.stderr)
        if self.include_dir:
            dir_name = os.path.dirname(path)
            base_name = os.path.basename(path)
            self.handle_file(dir_name, base_name, False)

    def handle_file(self, path: str, name: str, file_case: bool):
        # can only match the first line
        if not self.pattern.match(name):
            return
        new_name = self.pattern.sub(self.replacement, name)

        s = os.path.join(path, name)
        t = os.path.join(path, new_name)
        if os.path.exists(t):
            print(f"target file exists: {t}", file=sys.stderr)
            return
        if 0 < self.limit <= self.count_total:
            raise LimitException(f'reach limit {self.limit}')
        if file_case:
            self.count_file += 1
        else:
            self.count_dir += 1
        self.count_total += 1
        print(f"rename {s} to {new_name}")
        if self.effect:
            shutil.move(s, t)
        if self.content_pattern:
            self.handle_content(s, t)

    def handle_content(self, s: str, t: str):
        if self.effect:
            file = t
        else:
            file = s
        with open(file, 'rt') as r:
            text = r.read()
            if self.content_limit > 0:
                new_text = self.content_pattern.sub(
                    self.content_replacement, text, self.content_limit)
            else:
                new_text = self.content_pattern.sub(
                    self.content_replacement, text)
            if self.effect:
                with open(file, 'wt') as w:
                    w.write(new_text)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        description='rename file (and replace file content) by regexp')
    parser.add_argument('path', type=str, help='the directory path')
    parser.add_argument('-e', '--effect', action='store_true',
                        dest='effect', help='preform actually or not')
    parser.add_argument('-p', '--pattern', required=True,
                        dest='pattern', help='regexp pattern')
    parser.add_argument('-r', '--replacement', required=True,
                        dest='replacement',
                        help='target filename replacement')
    parser.add_argument('-D', '--include-dir', action='store_true',
                        dest='include_dir', help='rename dir')
    parser.add_argument('-F', '--include-file', action='store_true',
                        dest='include_file', help='rename file')
    parser.add_argument('-l', '--limit', type=int, default=-1,
                        dest='limit', help='limit to rename count')
    parser.add_argument('-P', '--content-pattern',
                        dest='content_pattern', help='pattern for content')
    parser.add_argument('-R', '--content-replacement',
                        dest='content_replacement',
                        help='target replacement for content')
    parser.add_argument('-L', '--content-limit', type=int, default=1,
                        dest='content_limit', help='limit to rename count')
    args = parser.parse_args()
    Main(args).run()
