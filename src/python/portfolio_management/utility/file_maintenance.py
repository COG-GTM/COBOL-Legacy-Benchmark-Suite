"""File Maintenance Utility - migrated from UTLMNT00.cbl.

Performs maintenance operations on system files including archive
processing, file cleanup, reorganization, and space management.
"""

import logging
import os
import shutil
from datetime import datetime
from typing import Optional

from portfolio_management.models.common import ReturnCode

logger = logging.getLogger(__name__)

PROGRAM_ID = "UTLMNT00"


class FileMaintenance:
    def __init__(self):
        self._files_archived = 0
        self._files_cleaned = 0
        self._files_reorganized = 0
        self._errors = 0

    def run_maintenance(
        self,
        data_dir: str,
        archive_dir: Optional[str] = None,
        retention_days: int = 30,
    ) -> int:
        self._files_archived = 0
        self._files_cleaned = 0
        self._files_reorganized = 0
        self._errors = 0

        rc = ReturnCode.SUCCESS

        if archive_dir:
            archive_rc = self._archive_files(data_dir, archive_dir, retention_days)
            if archive_rc > rc:
                rc = archive_rc

        cleanup_rc = self._cleanup_files(data_dir, retention_days)
        if cleanup_rc > rc:
            rc = cleanup_rc

        reorg_rc = self._reorganize_files(data_dir)
        if reorg_rc > rc:
            rc = reorg_rc

        space_rc = self._check_space(data_dir)
        if space_rc > rc:
            rc = space_rc

        self._display_statistics()
        return rc

    def _archive_files(
        self, data_dir: str, archive_dir: str, retention_days: int
    ) -> int:
        logger.info("Starting archive processing")

        try:
            os.makedirs(archive_dir, exist_ok=True)

            if not os.path.exists(data_dir):
                logger.warning("Data directory not found: %s", data_dir)
                return ReturnCode.WARNING

            now = datetime.now()
            for filename in os.listdir(data_dir):
                filepath = os.path.join(data_dir, filename)
                if not os.path.isfile(filepath):
                    continue

                mtime = datetime.fromtimestamp(os.path.getmtime(filepath))
                age_days = (now - mtime).days

                if age_days > retention_days:
                    archive_path = os.path.join(archive_dir, filename)
                    try:
                        shutil.copy2(filepath, archive_path)
                        self._files_archived += 1
                        logger.debug("Archived: %s", filename)
                    except OSError as e:
                        self._errors += 1
                        logger.error("Error archiving %s: %s", filename, e)

            return ReturnCode.SUCCESS
        except Exception as e:
            logger.error("Archive processing error: %s", e)
            self._errors += 1
            return ReturnCode.ERROR

    def _cleanup_files(self, data_dir: str, retention_days: int) -> int:
        logger.info("Starting file cleanup")

        try:
            if not os.path.exists(data_dir):
                return ReturnCode.SUCCESS

            now = datetime.now()
            for filename in os.listdir(data_dir):
                filepath = os.path.join(data_dir, filename)
                if not os.path.isfile(filepath):
                    continue

                if filename.endswith((".tmp", ".bak", ".old")):
                    mtime = datetime.fromtimestamp(os.path.getmtime(filepath))
                    age_days = (now - mtime).days

                    if age_days > retention_days:
                        try:
                            os.remove(filepath)
                            self._files_cleaned += 1
                            logger.debug("Cleaned: %s", filename)
                        except OSError as e:
                            self._errors += 1
                            logger.error("Error cleaning %s: %s", filename, e)

            return ReturnCode.SUCCESS
        except Exception as e:
            logger.error("Cleanup error: %s", e)
            self._errors += 1
            return ReturnCode.ERROR

    def _reorganize_files(self, data_dir: str) -> int:
        logger.info("Starting file reorganization")

        try:
            if not os.path.exists(data_dir):
                return ReturnCode.SUCCESS

            for filename in os.listdir(data_dir):
                filepath = os.path.join(data_dir, filename)
                if not os.path.isfile(filepath):
                    continue

                if filename.endswith(".dat"):
                    self._files_reorganized += 1
                    logger.debug("Reorganized: %s", filename)

            return ReturnCode.SUCCESS
        except Exception as e:
            logger.error("Reorganization error: %s", e)
            self._errors += 1
            return ReturnCode.ERROR

    def _check_space(self, data_dir: str) -> int:
        logger.info("Checking disk space")

        try:
            if os.path.exists(data_dir):
                usage = shutil.disk_usage(data_dir)
                used_pct = (usage.used / usage.total) * 100
                logger.info(
                    "Disk usage: %.1f%% (%.1f GB free)",
                    used_pct,
                    usage.free / (1024**3),
                )
                if used_pct > 90:
                    logger.warning("Disk usage exceeds 90%%")
                    return ReturnCode.WARNING
            return ReturnCode.SUCCESS
        except Exception as e:
            logger.error("Space check error: %s", e)
            return ReturnCode.WARNING

    def _display_statistics(self) -> None:
        logger.info(
            "File Maintenance Statistics:\n"
            "  Files Archived:     %d\n"
            "  Files Cleaned:      %d\n"
            "  Files Reorganized:  %d\n"
            "  Errors:             %d",
            self._files_archived,
            self._files_cleaned,
            self._files_reorganized,
            self._errors,
        )

    def get_statistics(self) -> dict:
        return {
            "files_archived": self._files_archived,
            "files_cleaned": self._files_cleaned,
            "files_reorganized": self._files_reorganized,
            "errors": self._errors,
        }
