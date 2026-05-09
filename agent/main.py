import os
from typing import Literal

import requests
from dotenv import load_dotenv
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

load_dotenv()

BASE_URL = os.getenv("BASE_URL")
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
MODEL_NAME = os.getenv("MODEL_NAME", "qwen-max")

app = FastAPI()

class ChatMessage(BaseModel):
    role: Literal["system", "user", "assistant"]
    content: str

class ChatReplyRequest(BaseModel):
    sessionId: int
    messages: list[ChatMessage]

class ChatReplyResponse(BaseModel):
    content: str


@app.post("/chat/reply", response_model=ChatReplyResponse)
def chat_reply(request: ChatReplyRequest):
    if not BASE_URL or not OPENAI_API_KEY:
        raise HTTPException(status_code=500, detail="LLM config is missing")

    payload = {
        "model": MODEL_NAME,
        "messages": [message.model_dump() for message in request.messages],
        "temperature": 0.2,
    }

    headers = {
        "Authorization": f"Bearer {OPENAI_API_KEY}",
        "Content-Type": "application/json",
    }

    try:
        response = requests.post(
            f"{BASE_URL}/chat/completions",
            json=payload,
            headers=headers,
            timeout=60,
        )
        response.raise_for_status()
        data = response.json()
        content = data["choices"][0]["message"]["content"]
        return ChatReplyResponse(content=content.strip())
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"LLM request failed: {exc}")