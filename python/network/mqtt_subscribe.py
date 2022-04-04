#!/usr/bin/env python3

import argparse
import paho.mqtt.client as client
import time


def parse_args():
    parser = argparse.ArgumentParser(
        description="publish some message to the network server")
    parser.add_argument('--topic', '-t', required=True,
                        help='the topic you wanna subscribe to')
    parser.add_argument('--hostname', '-H', default='127.0.0.1:61613',
                        help='the hostname of network server, default value is 127.0.0.1:61613')
    parser.add_argument('--username', '-u', default='admin',
                        help='username, default value is admin')
    parser.add_argument('--password', '-p', default='password',
                        help='password, default value is password')
    return parser.parse_args()


def on_connect_wrapper(topic):
    def on_connect(my_client, user_data, flags, result_code):
        print("Connected with result code " + str(result_code))
        my_client.subscribe(topic)
        print("Subscribed the topic: " + topic)

    return on_connect


def on_message(my_client, user_data, message):
    print("[{0}] {1}".format(message.topic, message.payload.decode("utf-8")))


def loop_forever():
    args = parse_args()
    topic = args.topic
    host = args.hostname.split(':')[0]
    port = int(args.hostname.split(':')[1])
    username = args.username
    password = args.password

    client_id = time.strftime('%Y%m%d%H%M%S', time.localtime(time.time()))
    my_client = client.Client(client_id)
    my_client.username_pw_set(username, password)
    my_client.on_connect = on_connect_wrapper(topic)
    my_client.on_message = on_message
    print("Trying to connect {0}:{1} with client_id {2}".format(host, port,
                                                                client_id))
    my_client.connect(host, port, keepalive=60)
    my_client.loop_forever()
