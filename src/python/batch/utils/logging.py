"""
Logging Configuration

Provides structured logging for batch processing programs.
Replaces DISPLAY statements and error logging from COBOL programs.
"""

import logging
import sys
from datetime import datetime
from typing import Optional

try:
    import structlog
    STRUCTLOG_AVAILABLE = True
except ImportError:
    STRUCTLOG_AVAILABLE = False


def setup_logging(
    level: int = logging.INFO,
    log_file: Optional[str] = None,
    use_structured: bool = True,
) -> None:
    """
    Set up logging configuration for batch processing.
    
    Args:
        level: Logging level (default: INFO)
        log_file: Optional file path for log output
        use_structured: Whether to use structured logging (if available)
    """
    handlers = [logging.StreamHandler(sys.stdout)]
    
    if log_file:
        handlers.append(logging.FileHandler(log_file))
    
    log_format = (
        "%(asctime)s - %(name)s - %(levelname)s - %(message)s"
    )
    
    logging.basicConfig(
        level=level,
        format=log_format,
        handlers=handlers,
    )
    
    if use_structured and STRUCTLOG_AVAILABLE:
        structlog.configure(
            processors=[
                structlog.stdlib.filter_by_level,
                structlog.stdlib.add_logger_name,
                structlog.stdlib.add_log_level,
                structlog.stdlib.PositionalArgumentsFormatter(),
                structlog.processors.TimeStamper(fmt="iso"),
                structlog.processors.StackInfoRenderer(),
                structlog.processors.format_exc_info,
                structlog.processors.UnicodeDecoder(),
                structlog.stdlib.ProcessorFormatter.wrap_for_formatter,
            ],
            context_class=dict,
            logger_factory=structlog.stdlib.LoggerFactory(),
            wrapper_class=structlog.stdlib.BoundLogger,
            cache_logger_on_first_use=True,
        )


def get_logger(name: str) -> logging.Logger:
    """
    Get a logger instance.
    
    Args:
        name: Logger name (typically __name__)
        
    Returns:
        Logger instance
    """
    if STRUCTLOG_AVAILABLE:
        return structlog.get_logger(name)
    return logging.getLogger(name)


class BatchLogger:
    """
    Batch processing logger with statistics tracking.
    
    Provides logging functionality similar to COBOL DISPLAY statements
    and error logging to ERRLOG table.
    """
    
    def __init__(self, program_id: str, log_file: Optional[str] = None):
        """
        Initialize batch logger.
        
        Args:
            program_id: Program identifier (8 characters max)
            log_file: Optional file path for log output
        """
        self.program_id = program_id[:8].ljust(8)
        self.logger = get_logger(program_id)
        self.start_time = datetime.now()
        self.message_count = 0
        self.error_count = 0
        self.warning_count = 0
        
        if log_file:
            handler = logging.FileHandler(log_file)
            handler.setFormatter(
                logging.Formatter("%(asctime)s - %(levelname)s - %(message)s")
            )
            self.logger.addHandler(handler)
    
    def info(self, message: str, **kwargs) -> None:
        """Log info message."""
        self.message_count += 1
        self.logger.info(message, **kwargs)
    
    def warning(self, message: str, **kwargs) -> None:
        """Log warning message."""
        self.message_count += 1
        self.warning_count += 1
        self.logger.warning(message, **kwargs)
    
    def error(self, message: str, **kwargs) -> None:
        """Log error message."""
        self.message_count += 1
        self.error_count += 1
        self.logger.error(message, **kwargs)
    
    def debug(self, message: str, **kwargs) -> None:
        """Log debug message."""
        self.logger.debug(message, **kwargs)
    
    def display(self, message: str) -> None:
        """
        Display message (COBOL DISPLAY equivalent).
        
        Prints to stdout and logs at INFO level.
        """
        print(f"{self.program_id}: {message}")
        self.info(message)
    
    def display_statistics(
        self,
        records_read: int = 0,
        records_processed: int = 0,
        records_written: int = 0,
        records_error: int = 0,
        return_code: int = 0,
    ) -> None:
        """
        Display processing statistics.
        
        Corresponds to statistics display in COBOL programs.
        """
        duration = (datetime.now() - self.start_time).total_seconds()
        
        stats = f"""
{self.program_id} Processing Statistics:
  Records Read:      {records_read}
  Records Processed: {records_processed}
  Records Written:   {records_written}
  Records Error:     {records_error}
  Return Code:       {return_code}
  Duration:          {duration:.2f} seconds
  Messages Logged:   {self.message_count}
  Warnings:          {self.warning_count}
  Errors:            {self.error_count}
"""
        print(stats)
        self.info(
            "Processing complete",
            records_read=records_read,
            records_processed=records_processed,
            records_written=records_written,
            records_error=records_error,
            return_code=return_code,
            duration_seconds=duration,
        )
