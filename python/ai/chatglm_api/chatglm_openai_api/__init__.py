#!/usr/bin/env python3
# -*- coding: utf-8 -*-
#
# Create by Jerry Will on 2023/12/02
#

"""
兼容 OpenAI ChatGPT接口

uvicorn --host 0.0.0.0 chatglm_openai_api:app --reload
"""
from typing import Annotated

from fastapi import Depends, FastAPI, Header

from .auth import ReqContext, get_current_user
from .chat_completions import ChatCompletionsReq, ChatCompletionsResp, \
    chat_completions

app = FastAPI()


@app.post("/health")
def _health(authorization: Annotated[str | None, Header()]) -> str:
    print(authorization)
    return "Hello World"


@app.post("/chat/completions")
def _chat_completions(req: ChatCompletionsReq,
        current_user: str = Depends(get_current_user)) -> ChatCompletionsResp:
    ctx = ReqContext(current_user)
    return chat_completions(req, ctx)
