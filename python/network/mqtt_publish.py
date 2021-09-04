#!/usr/bin/env python3

import argparse
# paho-network
import paho.mqtt.client as client
import paho.mqtt.publish as publish
import time


def on_connect(my_client, user_data, flags, result_code):
    print("Connected with result code " + str(result_code))


def parse_args():
    parser = argparse.ArgumentParser(
            description="publish some message to the network server")
    parser.add_argument('--topic', '-t', required=True,
                        help='the topic you wanna subscribe to')
    parser.add_argument('--hostname', '-H', default='127.0.0.1:61613',
                        help='the hostname of network server, default value is 127.0.0.1:61613')
    parser.add_argument('--username', '-u', help='username')
    parser.add_argument('--password', '-p', help='password')
    parser.add_argument('--message', '-m', nargs='+',
                        help='the messages to publish')
    parser.add_argument('--qos', '-q', type=int, choices=[0, 1, 2], default=0,
                        help='qos of messages')
    parser.add_argument('--retain', '-r', action='store_true',
                        help='retain of messages')
    return parser.parse_args()


def publish_single():
    args = parse_args()
    topic = args.topic
    host = args.hostname.split(':')[0]
    port = int(args.hostname.split(':')[1])
    username = args.username
    password = args.password
    messages = args.message
    qos = args.qos
    retain = True if args.retain else False

    client_id = time.strftime('%Y%m%d%H%M%S', time.localtime(time.time()))
    print("the messages: " + str(args.message))
    for message in messages:
        print("[{0}] {1} is published.".format(topic, message))
        publish.single(
                topic=topic, payload=message, qos=qos, retain=retain,
                hostname=host, port=port, client_id=client_id,
                auth={'username': username, 'password': password})


def client_publish():
    args = parse_args()
    topic = args.topic
    host = args.hostname.split(':')[0]
    port = int(args.hostname.split(':')[1])
    username = args.username
    password = args.password
    messages = args.message
    qos = args.qos
    retain = True if args.retain else False

    client_id = time.strftime('%Y%m%d%H%M%S', time.localtime(time.time()))
    print("the messages: " + str(args.message))
    my_client = client.Client(client_id)
    my_client.username_pw_set(username, password)
    my_client.on_connect = on_connect
    print("Trying to connect {0}:{1} with client_id {2}".format(host, port,
                                                                client_id))
    my_client.connect(host, port, keepalive=60)
    for message in messages:
        print("[{0}] {1} is published.".format(topic, message))
        my_client.publish(topic=topic, payload=message, qos=qos, retain=retain)


if __name__ == '__main__':
    publish_single()
