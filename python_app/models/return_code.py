"""Return Code Record model - translated from RTNCODE.cpy copybook.

Mirrors the COBOL return code handling from RTNCDE00.cbl with
standardized return code management across the system.
"""

from enum import StrEnum

from pydantic import BaseModel, Field


class ReturnCodeStatus(StrEnum):
    """Return code status from RTNCDE00.cbl 88-level values."""

    SUCCESS = "S"
    WARNING = "W"
    ERROR = "E"
    SEVERE = "F"


# Standard return code constants matching BCHCON.cpy
RC_SUCCESS = 0
RC_WARNING = 4
RC_ERROR = 8
RC_SEVERE = 12
RC_MAX_CONTINUE = 4  # RC <= 4 allows pipeline to continue


def classify_return_code(code: int) -> ReturnCodeStatus:
    """Classify a return code into a status category.

    Maps the EVALUATE in P200-SET-RETURN-CODE of RTNCDE00.cbl:
    - 0: SUCCESS
    - 1-4: WARNING
    - 5-8: ERROR
    - >8: SEVERE
    """
    if code == 0:
        return ReturnCodeStatus.SUCCESS
    if code <= 4:
        return ReturnCodeStatus.WARNING
    if code <= 8:
        return ReturnCodeStatus.ERROR
    return ReturnCodeStatus.SEVERE


class ReturnCodeRecord(BaseModel):
    """Return code record translated from COBOL RTNCODE copybook.

    Used by RTNCDE00.cbl for return code management, logging, and analysis.
    """

    program_id: str = Field(default="", max_length=8, description="RC-PROGRAM-ID")
    current_code: int = Field(default=0, description="RC-CURRENT-CODE")
    highest_code: int = Field(default=0, description="RC-HIGHEST-CODE")
    status: ReturnCodeStatus = Field(default=ReturnCodeStatus.SUCCESS, description="RC-STATUS")
    message: str = Field(default="", max_length=80, description="RC-MESSAGE")
    response_code: int = Field(default=0, description="RC-RESPONSE-CODE")

    def set_code(self, code: int) -> None:
        """Set return code and update highest/status (P200-SET-RETURN-CODE)."""
        self.current_code = code
        if code > self.highest_code:
            self.highest_code = code
        self.status = classify_return_code(code)
        self.response_code = 0

    def get_code(self) -> tuple[int, int, ReturnCodeStatus]:
        """Get current return code info (P300-GET-RETURN-CODE)."""
        return self.current_code, self.highest_code, self.status

    @property
    def can_continue(self) -> bool:
        """Check if pipeline can continue (RC <= 4 gating)."""
        return self.highest_code <= RC_MAX_CONTINUE
