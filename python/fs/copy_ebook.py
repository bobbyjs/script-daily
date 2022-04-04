#!/usr/bin/env python3
# coding=utf-8

"""
transplanted from kotlin script

source
    - kind
        - book
            - book_name.azw3
        - book_name.mobi
"""
import argparse
import os
import os.path
import sys
import re
import shutil


def any_match(s, patterns):
    for pattern in patterns:
        m = re.match(pattern, s)
        if m:
            return True
    return False


def main(args):
    source = os.path.abspath(args.source)
    target = os.path.abspath(args.target)
    patterns = args.patterns
    effect = args.effect

    if not os.path.isdir(source):
        print(f"source is not a directory: {source}", file=sys.stderr)
        return
    if not os.path.isdir(target):
        print(f"target is not a directory: {target}", file=sys.stderr)
        return
    if source.startswith(target) or target.startswith(source):
        print(f"included by each other: source={source}, target={target}",
              file=sys.stderr)
        return

    kinds = os.listdir(source)
    for kind in kinds:
        kind = os.path.join(source, kind)
        if not os.path.isdir(kind):
            continue
        target_kind = os.path.join(target, kind)
        books = os.listdir(kind)
        for book_name in books:
            book_path = os.path.join(kind, book_name)
            # copy to source/kind/book to target/kind/book
            if os.path.isfile(book_path):
                if not any_match(book_name, patterns):
                    continue
                target_book = os.path.join(target_kind, book_name)
                if os.path.exists(target_book):
                    print("already exists: {target_book}", file=sys.stderr)
                    continue
                if not os.path.exists(target_kind):
                    os.mkdir(target_kind)
                print(f"copy {book_path} to {target_book}")
                if effect:
                    shutil.copyfile(book_path, target_book)
            # copy to source/kind/book/file to target/kind/file
            elif os.path.isdir(book_path):
                handler_dir(book_path, target_kind, patterns, effect)


def handler_dir(book_path, target_kind, patterns, effect):
    files = os.listdir(book_path)
    for pattern in patterns:
        for file in files:
            file_path = os.path.join(book_path, file)
            if not os.path.isfile(book_path):
                continue
            if re.match(pattern, file):
                target_file = os.path.join(target_kind, file)
                print(f"copy {file_path} to {target_file}")
                if effect:
                    shutil.copyfile(file_path, target_file)
                return
    print(f"unmatched {book_path}")


if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        description='pick the preferred ebooks and copy them to another dir')
    parser.add_argument('-s', '--source', type=str, required=True,
                        dest='source', help='the source dir')
    parser.add_argument('-t', '--target', type=str, required=True,
                        dest='target', help='the target dir')
    parser.add_argument('-e', '--effect', action='store_true',
                        dest='effect', help='preform actually or not')
    parser.add_argument('patterns', nargs='+', help='regexp filenames')
    main(parser.parse_args())
