from typing import Any, Callable, Dict

from tools_schema import TOOL_SCHEMAS
from tools import get_current_time

AVAILABLE_TOOLS: Dict[str, Callable[..., Dict[str, Any]]] = {
    "get_current_time": get_current_time,
}


def get_tool_schemas() -> list[dict[str, Any]]:
    return TOOL_SCHEMAS


def execute_tool(tool_name: str, arguments: dict[str, Any] | None = None) -> Dict[str, Any]:
    if tool_name not in AVAILABLE_TOOLS:
        raise ValueError(f"Unsupported tool: {tool_name}")

    tool = AVAILABLE_TOOLS[tool_name]
    normalized_arguments = arguments or {}
    return tool(**normalized_arguments)
