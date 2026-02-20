"""DB2 Connection Manager - migrated from DB2CONN.cbl.

Manages database connections with retry logic, disconnect, and status checking.
"""

import logging
import time
from dataclasses import dataclass
from typing import Optional, Protocol

logger = logging.getLogger(__name__)

MAX_RETRIES = 3
RETRY_DELAY_SECONDS = 2


class DatabaseConnection(Protocol):
    def connect(self, dsn: str) -> None: ...
    def close(self) -> None: ...
    @property
    def is_connected(self) -> bool: ...


@dataclass
class ConnectionStatus:
    connected: bool = False
    retry_count: int = 0
    error_code: int = 0
    error_message: str = ""
    connection_token: str = ""


class DB2ConnectionManager:
    def __init__(self):
        self._connection: Optional[DatabaseConnection] = None
        self._status = ConnectionStatus()
        self._dsn: str = ""

    def connect(self, dsn: str, connection_factory: Optional[type] = None) -> int:
        self._dsn = dsn
        self._status.retry_count = 0

        while self._status.retry_count < MAX_RETRIES:
            try:
                if connection_factory is not None:
                    self._connection = connection_factory(dsn)
                self._status.connected = True
                self._status.error_code = 0
                self._status.error_message = ""
                self._status.connection_token = f"CONN-{id(self._connection)}"
                logger.info("DB2 connection established to %s", dsn)
                return 0
            except Exception as e:
                self._status.retry_count += 1
                self._status.error_code = -1
                self._status.error_message = str(e)
                logger.warning(
                    "DB2 connection attempt %d failed: %s",
                    self._status.retry_count,
                    e,
                )
                if self._status.retry_count < MAX_RETRIES:
                    time.sleep(RETRY_DELAY_SECONDS)

        logger.error(
            "DB2 connection failed after %d attempts", MAX_RETRIES
        )
        return 8

    def disconnect(self) -> int:
        if self._connection is not None:
            try:
                self._connection.close()
            except Exception as e:
                logger.error("Error disconnecting from DB2: %s", e)
                return 8

        self._status.connected = False
        self._status.connection_token = ""
        logger.info("DB2 connection closed")
        return 0

    def check_status(self) -> ConnectionStatus:
        return self._status

    @property
    def is_connected(self) -> bool:
        return self._status.connected

    @property
    def connection(self) -> Optional[DatabaseConnection]:
        return self._connection
