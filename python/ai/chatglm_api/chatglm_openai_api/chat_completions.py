import time
import uuid
from typing import List

from pydantic import BaseModel

from .auth import ReqContext
from .model import get_model


class ChatCompletionsMessage(BaseModel):
    role: str | None = None
    content: str


class ChatCompletionsReq(BaseModel):
    # 用于聊天完成的模型
    model: str | None = None
    message: List[ChatCompletionsMessage]  #
    # 采样温度，介于 0 和 2 之间。较高的值（如 0.8）将使输出更加随机，
    # 而较低的值（如 0.2）将使输出更加集中和确定。
    # 我们通常建议改变这个或top_p但不是两者。
    temperature: float | None = None
    # 核采样，考虑具有 top_p 概率质量的标记的结果。所以 0.1 意味着只考虑构成前 10% 概率质量的标记
    top_p: int | None = None
    # 默认为 1，为每个输入消息生成多少个聊天补全选择。
    n: int | None = None
    # 在聊天补全中生成的最大标记数。
    max_tokens: int | None = None
    # -2.0 和 2.0 之间的数字。正值会根据到目前为止是否出现在文本中来惩罚新标记，从而增加模型谈论新主题的可能性。
    presence_penalty: float | None = None
    # 默认为 0 -2.0 到 2.0 之间的数字。正值根据文本目前的存在频率惩罚新标记,降低模型重复相同行的可能性
    frequency_penalty: float | None = None


class ChatCompletionsRespChoice(BaseModel):
    index: int
    message: ChatCompletionsMessage


class ChatCompletionsRespUsage(BaseModel):
    completion_tokens: int  # 生成的完成中的标记数
    prompt_tokens: int  # 提示中的标记数
    total_tokens: int  # 请求中使用的标记总数(提示 + 完成)


class ChatCompletionsResp(BaseModel):
    id: str  # 聊天完成的唯一标识符
    choices: List[ChatCompletionsRespChoice] = []
    created: int  # 创建聊天完成的Unix时间戳(秒)
    model: str | None = None
    system_fingerprint: str | None = None  # 该指纹表示模型运行的后端配置
    object: str = "chat.completion"  # 对象类型,总是 chat.completion
    usage: ChatCompletionsRespUsage | None = None

    def __init__(self, id: str):
        created = int(time.time())
        super(ChatCompletionsResp, self).__init__(id=id, created=created)


def chat_completions(req: ChatCompletionsReq, ctx: ReqContext):
    id = str(uuid.uuid4())
    resp = ChatCompletionsResp(id)

    history = [msg.content for msg in req.message[:-1]]
    content = req.message[-1].content

    tokenizer, model = get_model()
    response, history = model.chat(tokenizer, content, history=history)
    print(f"response: {response}, history: {history}")
    choice = ChatCompletionsRespChoice(index=0, message=ChatCompletionsMessage(
        content=response))
    resp.choices = [choice]
    return resp
