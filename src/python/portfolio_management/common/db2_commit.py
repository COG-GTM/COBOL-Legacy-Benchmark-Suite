"""DB2 Commit Controller - migrated from DB2CMT.cbl.

Manages DB2 transactions including commit, rollback, savepoint creation,
and savepoint restoration.
"""

import logging
from dataclasses import dataclass
from typing import Optional, Protocol

logger = logging.getLogger(__name__)


class TransactionConnection(Protocol):
    def commit(self) -> None: ...
    def rollback(self) -> None: ...
    def execute(self, sql: str) -> None: ...


@dataclass
class CommitStatistics:
    commit_count: int = 0
    rollback_count: int = 0
    savepoint_count: int = 0
    restore_count: int = 0


class DB2CommitController:
    def __init__(self):
        self._connection: Optional[TransactionConnection] = None
        self._stats = CommitStatistics()
        self._initialized = False

    def initialize(self, connection: TransactionConnection) -> int:
        self._connection = connection
        self._stats = CommitStatistics()
        self._initialized = True
        logger.info("DB2 Commit Controller initialized")
        return 0

    def commit(self) -> int:
        if not self._initialized or self._connection is None:
            logger.error("Commit controller not initialized")
            return 8

        try:
            self._connection.commit()
            self._stats.commit_count += 1
            logger.debug("DB2 commit successful (total: %d)", self._stats.commit_count)
            return 0
        except Exception as e:
            logger.error("DB2 commit failed: %s", e)
            return 8

    def rollback(self) -> int:
        if not self._initialized or self._connection is None:
            logger.error("Commit controller not initialized")
            return 8

        try:
            self._connection.rollback()
            self._stats.rollback_count += 1
            logger.warning("DB2 rollback performed (total: %d)", self._stats.rollback_count)
            return 0
        except Exception as e:
            logger.error("DB2 rollback failed: %s", e)
            return 12

    def create_savepoint(self, name: str) -> int:
        if not self._initialized or self._connection is None:
            logger.error("Commit controller not initialized")
            return 8

        try:
            self._connection.execute(f"SAVEPOINT {name} ON ROLLBACK RETAIN CURSORS")
            self._stats.savepoint_count += 1
            logger.debug("Savepoint '%s' created", name)
            return 0
        except Exception as e:
            logger.error("Create savepoint '%s' failed: %s", name, e)
            return 8

    def restore_savepoint(self, name: str) -> int:
        if not self._initialized or self._connection is None:
            logger.error("Commit controller not initialized")
            return 8

        try:
            self._connection.execute(f"ROLLBACK TO SAVEPOINT {name}")
            self._stats.restore_count += 1
            logger.info("Savepoint '%s' restored", name)
            return 0
        except Exception as e:
            logger.error("Restore savepoint '%s' failed: %s", name, e)
            return 8

    def get_statistics(self) -> CommitStatistics:
        return self._stats

    def terminate(self) -> int:
        logger.info(
            "DB2 Commit Controller stats - Commits: %d, Rollbacks: %d, "
            "Savepoints: %d, Restores: %d",
            self._stats.commit_count,
            self._stats.rollback_count,
            self._stats.savepoint_count,
            self._stats.restore_count,
        )
        self._initialized = False
        return 0
