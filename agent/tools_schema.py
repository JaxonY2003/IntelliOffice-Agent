from typing import Any, Dict, List

TOOL_SCHEMAS: List[Dict[str, Any]] = [
    {
        "type": "function",
        "function": {
            "name": "get_current_time",
            "description": "Get the current date and time for the specified timezone.",
            "parameters": {
                "type": "object",
                "properties": {
                    "timezone": {
                        "type": "string",
                        "description": "IANA timezone name, for example Asia/Shanghai.",
                    }
                },
                "required": [],
            }
        }
    }
]
