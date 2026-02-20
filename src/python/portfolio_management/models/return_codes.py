"""Return Code Management - migrated from RTNCODE.cpy."""

from dataclasses import dataclass
from enum import Enum


class ReturnCodeRequestType(str, Enum):
    INIT = "INIT"
    SET = "SET"
    GET = "GET"
    LOG = "LOG"
    ANAL = "ANAL"


@dataclass
class ReturnCodeRequest:
    request_type: str = ReturnCodeRequestType.INIT
    program_id: str = ""
    return_code: int = 0
    highest_code: int = 0
    status: str = ""
    message: str = ""
