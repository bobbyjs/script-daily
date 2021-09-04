#!/usr/bin/env python3

import argparse
import redis


class Main(object):
    def __init__(self, host, port):
        self.host = host
        self.port = port
        self.db = redis.Redis(host=host, port=port)

    def run(self, file, type, key):
        for line in open(file):
            self.push(type, key, line)

    def push(self, type, key, value):
        if type == 'list':
            self.db.lpush(key, value)
        elif type == 'set':
            self.db.sadd(key, value)
        elif type == 'zset':
            self.db.zadd(key, value)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('-H', '--host',
                        dest='host', default='127.0.0.1',
                        help='redis host, default is 127.0.0.1')
    parser.add_argument('-p', '--port',
                        dest='port', default=6379,
                        help='redis port, default is 6379')
    parser.add_argument('-f', '--file',
                        dest='file', help='per line as a value')
    parser.add_argument('-t', '--type',
                        dest='type', default='list',
                        choices=['list', 'set', 'zset'],
                        help='redis type, one of ["list", "set", "zset"]')
    parser.add_argument('-k', '--key',
                        dest='key', help='redis key')
    args = parser.parse_args()
    Main(args.host, args.port).run(str(args.file), args.type.lower(), args.key)
