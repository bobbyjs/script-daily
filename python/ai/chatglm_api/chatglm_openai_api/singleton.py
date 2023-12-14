import threading


class LazySingleton:

    def __init__(self, _instance_gen):
        self._instance_lock = threading.Lock()
        self._instance_gen = _instance_gen

    def get_instance(self):
        if not hasattr(self, "_instance"):
            with self._instance_lock:
                if not hasattr(self, "_instance"):
                    self._instance = self._instance_gen()
        return self._instance
