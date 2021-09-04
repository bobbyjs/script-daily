#!/usr/bin/env python3
# coding=utf-8

import pika


class RabbitMQUtil:
    def connect_vhost(self, url):
        connection = pika.BlockingConnection(pika.URLParameters(url))
        self.channel = connection.channel()

    def connect(self, host='localhost', port=5672, username=None,
            password=None):
        # amqp://username:password@localhost:5672
        if host.startswith("amqp://"):
            host = host[len("amqp://"):]
            if "@" in host:
                username = host.split(":")[0]
                host = ':'.join(host.split(":")[1:])
                password = host.split("@")[0]
                host = host.split("@")[1]

            port = host.split(":")[1]
            port = int(port)
            host = host.split(":")[0]
        connection = pika.BlockingConnection(pika.ConnectionParameters(
                host=host, port=port, username=username, password=password))
        self.channel = connection.channel()

    def send(self, queue_name, message, exchange_name='', ):
        self.channel.basic_publish(
                exchange=exchange_name,
                routing_key=queue_name,
                body=message,
                properties=pika.spec.BasicProperties(content_type="text/plain"))

    def receive(self, queue_name):
        self.channel.basic_consume(
                queue=queue_name,
                on_message_callback=self.callback,
                auto_ack=True)
        self.channel.start_consuming()

    def migrate(self, queue_name1, queue_name2):
        self.channel.basic_consume(
                queue=queue_name1,
                on_message_callback=self.callback_migrate(queue_name2),
                auto_ack=True)
        self.channel.start_consuming()

    def callback(self, channel, method, properties, body):
        print("%s" % body.decode())

    # migrate to queue_name
    def callback_migrate(self, queue_name):
        def callback(channel, method, properties, body):
            print("%s" % body.decode())
            self.channel.basic_publish(
                    exchange='',
                    routing_key=queue_name,
                    body=body,
                    properties=properties)

        return callback

    @staticmethod
    def help():
        import sys
        print(
                f"Usage: {sys.argv[0]} amqp://username:password@localhost:5672/vhost \\")
        print(
                f"\treceive <queue>")
        print(
                "\tsend <queue> <message>...")
        print(
                f"\tmigrate <queue1> <queue2>")
        sys.exit(1)


# ./rabbitmq-util.py amqp://root:root@127.0.0.1:5672 receive hello
if __name__ == '__main__':
    import sys

    util = RabbitMQUtil()
    args = sys.argv
    if len(args) < 4:
        util.help()

    some_url, some_cmd, some_queue_name = args[1], args[2], args[3]

    util.connect_vhost(some_url)
    if some_cmd.lower() in ["send", "s"]:
        if len(args) < 5:
            util.help()

        messages = args[4:]
        for message_body in messages:
            print(message_body)
            util.send(some_queue_name, message_body)
    elif some_cmd.lower() in ["receive", "r"]:
        util.receive(some_queue_name)
    else:
        util.help()
