"""Online DB2 Connection Manager - migrated from DB2ONLN.cbl.

Manages DB2 connection pool, optimizes connection reuse, handles
connection errors, and monitors connection status.
"""

import logging
from dataclasses import dataclass
from datetime import datetime
from typing import Optional

from portfolio_management.models.common import ReturnCode

logger = logging.getLogger(__name__)

PROGRAM_ID = "DB2ONLN"
MAX_CONNECTIONS = 100


@dataclass
class ConnectionInfo:
    token: str = ""
    user_id: str = ""
    connected_at: str = ""
    last_used: str = ""
    active: bool = False


class DB2OnlineManager:
    def __init__(self, max_connections: int = MAX_CONNECTIONS):
        self._connections: dict[str, ConnectionInfo] = {}
        self._max_connections = max_connections
        self._active_count = 0

    def connect(self, user_id: str) -> tuple[int, str]:
        if self._active_count >= self._max_connections:
            logger.error("Connection pool exhausted (%d/%d)", self._active_count, self._max_connections)
            return ReturnCode.ERROR, ""

        token = f"CONN-{user_id}-{datetime.now().strftime('%H%M%S%f')}"
        now = datetime.now().strftime("%Y-%m-%d-%H.%M.%S.%f")

        info = ConnectionInfo(
            token=token,
            user_id=user_id,
            connected_at=now,
            last_used=now,
            active=True,
        )
        self._connections[token] = info
        self._active_count += 1

        logger.debug("DB2 online connection established: %s (active: %d)", token, self._active_count)
        return ReturnCode.SUCCESS, token

    def disconnect(self, token: str) -> int:
        info = self._connections.get(token)
        if info is None:
            logger.warning("Connection token not found: %s", token)
            return ReturnCode.WARNING

        if info.active:
            info.active = False
            info.last_used = datetime.now().strftime("%Y-%m-%d-%H.%M.%S.%f")
            self._active_count -= 1

        logger.debug("DB2 online connection closed: %s (active: %d)", token, self._active_count)
        return ReturnCode.SUCCESS

    def check_status(self, token: str) -> Optional[ConnectionInfo]:
        return self._connections.get(token)

    def get_active_count(self) -> int:
        return self._active_count

    def cleanup_idle(self, max_idle_seconds: int = 300) -> int:
        cleaned = 0
        now = datetime.now()
        for token, info in list(self._connections.items()):
            if not info.active:
                try:
                    last_used_time = datetime.strptime(info.last_used, "%Y-%m-%d-%H.%M.%S.%f")
                    idle_seconds = (now - last_used_time).total_seconds()
                    if idle_seconds < max_idle_seconds:
                        continue
                except (ValueError, TypeError):
                    pass
                del self._connections[token]
                cleaned += 1

        if cleaned > 0:
            logger.info("Cleaned up %d idle connections", cleaned)
        return cleaned
