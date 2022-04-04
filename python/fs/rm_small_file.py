#!/usr/bin/env python3
# coding=utf-8
import argparse
import os
import os.path
import sys


def fmt_size(n: int):
    if n < (1 << 10):
        return "%dB" % n
    elif n < (1 << 20):
        return "%.2fK" % (n / (1 << 10))
    elif n < (1 << 30):
        return "%.2fM" % (n / (1 << 20))
    else:
        return "%.2fG" % (n / (1 << 30))


class Main:
    count_dir: int = 0
    count_file: int = 0
    count_effect_dir: int = 0
    count_rm_size: int = 0

    def __init__(self, args: argparse.Namespace):
        self.path = args.path
        self.size = args.size
        self.effect = args.effect
        self.prune_empty_dir = args.prune_empty_dir

    def run(self):
        p = os.path.abspath(self.path)
        if not os.path.isdir(p):
            print(f"not a directory: {p}", file=sys.stderr)
            return
        self.handle_dir(p)
        print(f"rm {self.count_file} files, {self.count_dir} dir, "
              f"effect {self.count_effect_dir} dir")
        print(f"total rm size {fmt_size(self.count_rm_size)}")

    def handle_dir(self, d: str):
        try:
            files = os.listdir(d)
        except PermissionError:
            print(f"no permission on {d}")
            return
        for f in files:
            f = os.path.join(d, f)
            if os.path.isdir(f):
                self.handle_dir(f)
            elif os.path.isfile(f):
                self.handle_file(f)
            else:
                print(f"neither a file or dir: {d}", file=sys.stderr)
        if len(os.listdir(d)) == 0:
            print(f"rmdir {d}")
            self.count_effect_dir += 1
            self.count_dir += 1
            if self.prune_empty_dir:
                # shutil.rmtree()
                os.rmdir(d)

    def handle_file(self, f: str):
        n = os.path.getsize(f)
        if n > self.size:
            return
        print(f"rm {f}, size = {n}")
        self.count_file += 1
        self.count_rm_size += n
        if self.effect:
            os.remove(f)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='rm small files')
    parser.add_argument('path', type=str, help='the directory path')
    parser.add_argument('-s', '--size', type=int, required=True,
                        dest='size', help='the min size of files to remain')
    parser.add_argument('-e', '--effect', action='store_true',
                        dest='effect', help='preform actually or not')
    parser.add_argument('--prune-empty-dir', action='store_true',
                        dest='prune_empty_dir', help='prune the empty dirs')
    main = Main(parser.parse_args())
    main.run()
