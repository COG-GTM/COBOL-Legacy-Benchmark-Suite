"""Error Handling Definitions - migrated from ERRHAND.cpy and RETHND.cpy."""

from dataclasses import dataclass
from enum import Enum


class ErrorCategory(str, Enum):
    VSAM = "VS"
    VALIDATION = "VL"
    PROCESSING = "PR"
    SYSTEM = "SY"


class ErrorReturnCode:
    SUCCESS = 0
    WARNING = 4
    ERROR = 8
    SEVERE = 12
    TERMINAL = 16


@dataclass
class ErrorMessage:
    date: str = ""
    time: str = ""
    program: str = ""
    category: str = ""
    code: str = ""
    severity: int = 0
    text: str = ""
    details: str = ""


class ErrorType(str, Enum):
    VALIDATION = "V"
    PROCESSING = "P"
    DATABASE = "D"
    FILE = "F"
    SECURITY = "S"


class ActionFlag(str, Enum):
    CONTINUE = "C"
    ABORT = "A"
    RETRY = "R"


@dataclass
class ReturnHandling:
    return_code: int = 0
    reason_code: int = 0
    module_id: str = ""
    function_id: str = ""
    program_name: str = ""
    paragraph_name: str = ""
    error_routine: str = ""
    error_type: str = ""
    error_code: str = ""
    error_text: str = ""
    system_code: str = ""
    system_msg: str = ""
    action_flag: str = ActionFlag.CONTINUE
    retry_count: int = 0
    max_retries: int = 3


class StandardErrorCode:
    INVALID_DATA = "E001"
    NOT_FOUND = "E002"
    DUPLICATE = "E003"
    FILE_ERROR = "E004"
    DB_ERROR = "E005"
    SECURITY = "E006"
    PROCESSING = "E007"
    VALIDATION = "E008"
    VERSION = "E009"
    TIMEOUT = "E010"
