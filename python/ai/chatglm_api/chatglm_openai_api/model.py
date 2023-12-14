import os
import time

import torch
from transformers import AutoModel, AutoTokenizer

from ._async import submit_async
from .singleton import LazySingleton

MODEL_PATH = os.environ.get('MODEL_PATH', 'THUDM/chatglm3-6b')
TOKENIZER_PATH = os.environ.get("TOKENIZER_PATH", MODEL_PATH)
DEVICE = 'cuda' if torch.cuda.is_available() else 'cpu'


def _get_model():
    print("start to get model")
    tokenizer = AutoTokenizer.from_pretrained(TOKENIZER_PATH,
                                              trust_remote_code=True)
    if 'cuda' in DEVICE:  # AMD, NVIDIA GPU can use Half Precision
        model = AutoModel.from_pretrained(MODEL_PATH, trust_remote_code=True) \
            .quantize(4).to(DEVICE).eval()
    else:  # CPU, Intel GPU and other GPU can use Float16 Precision Only
        model = AutoModel.from_pretrained(MODEL_PATH, trust_remote_code=True) \
            .float().to(DEVICE).eval()
    print("end to get model")
    return tokenizer, model


_model_getter = LazySingleton(_get_model)


def get_model():
    tokenizer, model = _model_getter.get_instance()
    return tokenizer, model


submit_async(lambda: (
    print("submit_async get_model"),
    time.sleep(3),
    get_model()
))
