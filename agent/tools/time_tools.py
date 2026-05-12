from datetime import datetime
from zoneinfo import ZoneInfo

def get_current_time(timezone: str = "Asia/Shanghai") -> dict:
    now = datetime.now(ZoneInfo(timezone))
    return {
        "timezone": timezone,
        "iso_time": now.isoformat(),
        "formatted_time": now.strftime("%Y-%m-%d %H:%M:%S"),
    }