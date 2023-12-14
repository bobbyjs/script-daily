import logging
import time
import traceback
from concurrent.futures import ThreadPoolExecutor


pool = ThreadPoolExecutor()

log = logging.Logger(__name__)


def submit_async(f, *arg, **kwargs):
    submitted_at = time.time()

    def task():
        called_at = time.time()
        print(f"start to run async task, wait cost: {time.time() - submitted_at}s")
        try:
            f(*arg, **kwargs)
        except Exception:
            print(traceback.format_exc())
        print(f"end to run async task, call cost: {time.time() - called_at}s")
    pool.submit(task)
