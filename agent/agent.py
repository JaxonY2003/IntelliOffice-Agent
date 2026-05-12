import json
from typing import Any

import requests

from config import BASE_URL, MODEL_NAME, OPENAI_API_KEY
from prompts import build_system_prompt
from schemas import ChatMessage, ChatReplyRequest
from tools_registry import execute_tool, get_tool_schemas


TIME_QUERY_KEYWORDS = (
    "现在几点",
    "几点了",
    "当前时间",
    "现在时间",
    "现在几号",
    "今天几号",
    "今天星期几",
    "今天周几",
    "当前日期",
)


def generate_reply(request: ChatReplyRequest) -> str:
    messages = build_messages(request.messages)
    last_user_message = extract_latest_user_message(request.messages)

    try:
        first_response = call_llm(messages, tools=get_tool_schemas())
    except Exception:
        if is_time_query(last_user_message):
            return build_time_fallback_reply()
        raise

    assistant_message = extract_assistant_message(first_response)
    tool_calls = assistant_message.get("tool_calls") or []

    if tool_calls:
        messages.append(build_assistant_tool_call_message(assistant_message))

        for tool_call in tool_calls:
            messages.append(execute_tool_call(tool_call))

        second_response = call_llm(messages)
        final_message = extract_assistant_message(second_response)
        final_content = extract_text_content(final_message)
        if final_content:
            return final_content

    if is_time_query(last_user_message):
        return build_time_fallback_reply()

    direct_content = extract_text_content(assistant_message)
    if direct_content:
        return direct_content

    raise RuntimeError("LLM returned an empty reply")


def build_messages(request_messages: list[ChatMessage]) -> list[dict[str, Any]]:
    messages: list[dict[str, Any]] = [
        {
            "role": "system",
            "content": build_system_prompt(),
        }
    ]
    messages.extend(message.model_dump() for message in request_messages)
    return messages


def call_llm(messages: list[dict[str, Any]], tools: list[dict[str, Any]] | None = None) -> dict[str, Any]:
    if not BASE_URL or not OPENAI_API_KEY:
        raise RuntimeError("LLM config is missing")

    payload: dict[str, Any] = {
        "model": MODEL_NAME,
        "messages": messages,
        "temperature": 0.2,
    }
    if tools:
        payload["tools"] = tools
        payload["tool_choice"] = "auto"

    headers = {
        "Authorization": f"Bearer {OPENAI_API_KEY}",
        "Content-Type": "application/json",
    }

    response = requests.post(
        f"{BASE_URL}/chat/completions",
        json=payload,
        headers=headers,
        timeout=60,
    )
    response.raise_for_status()
    return response.json()


def extract_assistant_message(response_data: dict[str, Any]) -> dict[str, Any]:
    choices = response_data.get("choices") or []
    if not choices:
        raise RuntimeError("LLM response does not contain choices")

    message = choices[0].get("message")
    if not isinstance(message, dict):
        raise RuntimeError("LLM response message is missing")

    return message


def execute_tool_call(tool_call: dict[str, Any]) -> dict[str, Any]:
    function_payload = tool_call.get("function") or {}
    tool_name = function_payload.get("name")
    if not tool_name:
        raise RuntimeError("Tool call does not contain a function name")

    raw_arguments = function_payload.get("arguments") or "{}"
    try:
        arguments = json.loads(raw_arguments)
    except json.JSONDecodeError as exc:
        raise RuntimeError(f"Tool arguments are not valid JSON: {raw_arguments}") from exc

    result = execute_tool(tool_name, arguments)
    return {
        "role": "tool",
        "tool_call_id": tool_call.get("id", ""),
        "content": json.dumps(result, ensure_ascii=False),
    }


def build_assistant_tool_call_message(assistant_message: dict[str, Any]) -> dict[str, Any]:
    return {
        "role": "assistant",
        "content": assistant_message.get("content") or "",
        "tool_calls": assistant_message.get("tool_calls") or [],
    }


def extract_latest_user_message(messages: list[ChatMessage]) -> str:
    for message in reversed(messages):
        if message.role == "user":
            return message.content.strip()
    return ""


def is_time_query(content: str) -> bool:
    normalized_content = content.strip()
    if not normalized_content:
        return False

    return any(keyword in normalized_content for keyword in TIME_QUERY_KEYWORDS)


def build_time_fallback_reply() -> str:
    result = execute_tool("get_current_time")
    return f"现在是 {result['formatted_time']}，时区为 {result['timezone']}。"


def extract_text_content(message: dict[str, Any]) -> str:
    content = message.get("content")
    if isinstance(content, str):
        return content.strip()

    if isinstance(content, list):
        text_parts = []
        for item in content:
            if isinstance(item, dict) and item.get("type") == "text":
                text = item.get("text")
                if isinstance(text, str) and text.strip():
                    text_parts.append(text.strip())
        return "\n".join(text_parts).strip()

    return ""
