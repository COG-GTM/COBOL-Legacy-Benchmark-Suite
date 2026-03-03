"""File Maintenance module - replaces UTLMNT00.cbl.

Provides file maintenance operations: archive, cleanup, reorganize, analyze.

COBOL program flow (EVALUATE LS-MNT-FUNCTION):
- ARCHIVE: Archive old data files (P100-ARCHIVE)
- CLEANUP: Remove expired/temporary data (P200-CLEANUP)
- REORG: Reorganize data files (P300-REORG)
- ANALYZE: Analyze file statistics (P400-ANALYZE)
"""

import logging
import os
import shutil
from datetime import datetime
from pathlib import Path
from typing import Any

logger = logging.getLogger("portfolio.utils.maintenance")


class MaintenanceProcessor:
    """File maintenance processor replacing UTLMNT00.cbl."""

    def __init__(self, base_path: str = "/tmp/portfolio") -> None:
        self.base_path = Path(base_path)
        self.operations_log: list[dict[str, Any]] = []
        self.files_processed = 0
        self.files_archived = 0
        self.files_cleaned = 0
        self.bytes_freed = 0

    def archive(
        self,
        source_dir: str,
        archive_dir: str,
        days_old: int = 30,
    ) -> dict[str, Any]:
        """Archive old data files - replaces P100-ARCHIVE.

        COBOL: Moves files older than retention period to archive.
        """
        result = {
            "operation": "ARCHIVE",
            "source": source_dir,
            "archive": archive_dir,
            "days_old": days_old,
            "files_archived": 0,
            "bytes_archived": 0,
            "timestamp": datetime.now().isoformat(),
        }

        source = Path(source_dir)
        archive = Path(archive_dir)

        if not source.exists():
            result["status"] = "SOURCE_NOT_FOUND"
            self._log_operation(result)
            return result

        archive.mkdir(parents=True, exist_ok=True)
        cutoff = datetime.now().timestamp() - (days_old * 86400)

        for file_path in source.iterdir():
            if file_path.is_file() and file_path.stat().st_mtime < cutoff:
                dest = archive / file_path.name
                shutil.move(str(file_path), str(dest))
                result["files_archived"] += 1
                result["bytes_archived"] += dest.stat().st_size
                self.files_archived += 1

        result["status"] = "COMPLETE"
        self._log_operation(result)
        return result

    def cleanup(
        self,
        target_dir: str,
        pattern: str = "*.tmp",
    ) -> dict[str, Any]:
        """Remove expired/temporary data - replaces P200-CLEANUP.

        COBOL: Deletes temporary work files and expired data.
        """
        result = {
            "operation": "CLEANUP",
            "target": target_dir,
            "pattern": pattern,
            "files_removed": 0,
            "bytes_freed": 0,
            "timestamp": datetime.now().isoformat(),
        }

        target = Path(target_dir)
        if not target.exists():
            result["status"] = "TARGET_NOT_FOUND"
            self._log_operation(result)
            return result

        for file_path in target.glob(pattern):
            if file_path.is_file():
                size = file_path.stat().st_size
                file_path.unlink()
                result["files_removed"] += 1
                result["bytes_freed"] += size
                self.files_cleaned += 1
                self.bytes_freed += size

        result["status"] = "COMPLETE"
        self._log_operation(result)
        return result

    def reorg(self, target_dir: str) -> dict[str, Any]:
        """Reorganize data files - replaces P300-REORG.

        COBOL: VSAM REPRO/IDCAMS REORGANIZE equivalent.
        In Python/PostgreSQL this maps to VACUUM/REINDEX.
        """
        result = {
            "operation": "REORG",
            "target": target_dir,
            "timestamp": datetime.now().isoformat(),
            "status": "COMPLETE",
            "message": "Database reorganization (VACUUM/REINDEX) should be run via PostgreSQL",
        }
        self._log_operation(result)
        return result

    def analyze(self, target_dir: str) -> dict[str, Any]:
        """Analyze file statistics - replaces P400-ANALYZE.

        COBOL: Collects file statistics (record counts, sizes, fragmentation).
        """
        result: dict[str, Any] = {
            "operation": "ANALYZE",
            "target": target_dir,
            "timestamp": datetime.now().isoformat(),
            "files": [],
            "total_files": 0,
            "total_size": 0,
        }

        target = Path(target_dir)
        if not target.exists():
            result["status"] = "TARGET_NOT_FOUND"
            self._log_operation(result)
            return result

        for file_path in sorted(target.iterdir()):
            if file_path.is_file():
                stat = file_path.stat()
                result["files"].append({
                    "name": file_path.name,
                    "size": stat.st_size,
                    "modified": datetime.fromtimestamp(stat.st_mtime).isoformat(),
                })
                result["total_files"] += 1
                result["total_size"] += stat.st_size

        result["status"] = "COMPLETE"
        self._log_operation(result)
        return result

    def _log_operation(self, result: dict[str, Any]) -> None:
        """Log a maintenance operation."""
        self.operations_log.append(result)
        self.files_processed += 1
        logger.info(
            "UTLMNT00 %s: status=%s",
            result.get("operation", "UNKNOWN"),
            result.get("status", "UNKNOWN"),
        )

    def get_summary(self) -> dict[str, Any]:
        """Get maintenance operations summary."""
        return {
            "operations_count": len(self.operations_log),
            "files_archived": self.files_archived,
            "files_cleaned": self.files_cleaned,
            "bytes_freed": self.bytes_freed,
        }
