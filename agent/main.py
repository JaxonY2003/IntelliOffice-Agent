from fastapi import FastAPI, HTTPException

from agent import generate_reply
from schemas import ChatReplyRequest, ChatReplyResponse

app = FastAPI()


@app.post("/chat/reply", response_model=ChatReplyResponse)
def chat_reply(request: ChatReplyRequest):
    try:
        content = generate_reply(request)
        return ChatReplyResponse(content=content)
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"LLM request failed: {exc}")
