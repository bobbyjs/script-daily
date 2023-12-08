#!/usr/bin/env python3
# -*- coding: utf-8 -*-
#
# Create by Jerry Will on 2023/12/02
#

'''
兼容 OpenAI ChatGPT接口
https://fastapi.tiangolo.com/zh/tutorial/security/oauth2-jwt/
'''
from typing import Annotated, List, Optional

from fastapi import FastAPI, Header, Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer
from pydantic import BaseModel

app = FastAPI()

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="token")

async def get_current_user(token: str = Depends(oauth2_scheme)):
    credentials_exception = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Could not validate credentials",
        headers={"WWW-Authenticate": "Bearer"},
    )
    if not token or "" != token:
        raise credentials_exception
    return user


class ChatCompletionsMessage(BaseModel):
    role: str
    content: str


class ChatCompletionsReq:
    # 用于聊天完成的模型
    model: str
    message: List[ChatCompletionsMessage] #
    # 采样温度，介于 0 和 2 之间。较高的值（如 0.8）将使输出更加随机，
    # 而较低的值（如 0.2）将使输出更加集中和确定。
    # 我们通常建议改变这个或top_p但不是两者。
    temperature: Optional[float]
    # 核采样，考虑具有 top_p 概率质量的标记的结果。所以 0.1 意味着只考虑构成前 10% 概率质量的标记
    top_p: Optional[int]
    # 默认为 1，为每个输入消息生成多少个聊天补全选择。
    n: Optional[int]
    # 在聊天补全中生成的最大标记数。
    max_tokens: Optional[int]
    # -2.0 和 2.0 之间的数字。正值会根据到目前为止是否出现在文本中来惩罚新标记，从而增加模型谈论新主题的可能性。
    presence_penalty: Optional[float]
    # 默认为 0 -2.0 到 2.0 之间的数字。正值根据文本目前的存在频率惩罚新标记,降低模型重复相同行的可能性
    frequency_penalty: Optional[float]


class ChatCompletionsRespChoices(BaseModel):
    index: int
    message: ChatCompletionsMessage


class ChatCompletionsRespUsage(BaseModel):
    completion_tokens: int # 生成的完成中的标记数
    prompt_tokens: int # 提示中的标记数
    total_tokens: int # 请求中使用的标记总数(提示 + 完成)


class ChatCompletionsResp(BaseModel):
    id: str # 聊天完成的唯一标识符
    choices: List[ChatCompletionsRespChoices]
    created: int # 创建聊天完成的Unix时间戳(秒)
    model: str
    system_fingerprint: str # 该指纹表示模型运行的后端配置
    object: str # 对象类型,总是 chat.completion
    usage: ChatCompletionsRespUsage


@app.post("/chat/completions")
def chat_completions(req: ChatCompletionsReq, auth: Annotated[str | None, Header()]) -> ChatCompletionsResp:
    resp = ChatCompletionsResp()
    return resp

