from typing import Literal

from pydantic import BaseModel


class ChatMessage(BaseModel):
    role: Literal["system", "user", "assistant"]
    content: str


class ChatReplyRequest(BaseModel):
    sessionId: int
    messages: list[ChatMessage]


class ChatReplyResponse(BaseModel):
    content: str
