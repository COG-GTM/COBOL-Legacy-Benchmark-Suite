"""
Pydantic v2 models for COBOL RETHND copybook (Return Code Handling Definitions).

Source: src/copybook/common/RETHND.cpy
"""

from pydantic import BaseModel, Field, field_validator


class ReturnStatus(BaseModel):
    """Return status from RETURN-STATUS (level 05)."""

    model_config = {"from_attributes": True}

    return_code: int = Field(
        description=(
            "Return code. COBOL: RETURN-CODE PIC S9(4) COMP. "
            "88-level values: 0=Success, 4=Warning, 8=Error, 12=Severe, 16=Critical."
        ),
    )
    reason_code: int = Field(
        description="Reason code. COBOL: REASON-CODE PIC S9(4) COMP.",
    )
    module_id: str = Field(
        max_length=8,
        description="Module identifier. COBOL: MODULE-ID PIC X(8).",
    )
    function_id: str = Field(
        max_length=8,
        description="Function identifier. COBOL: FUNCTION-ID PIC X(8).",
    )


class ErrorLocation(BaseModel):
    """Error location from ERROR-LOCATION (level 10)."""

    model_config = {"from_attributes": True}

    program_name: str = Field(
        max_length=8,
        description="Program name. COBOL: PROGRAM-NAME PIC X(8).",
    )
    paragraph_name: str = Field(
        max_length=8,
        description="Paragraph name. COBOL: PARAGRAPH-NAME PIC X(8).",
    )
    error_routine: str = Field(
        max_length=8,
        description="Error routine. COBOL: ERROR-ROUTINE PIC X(8).",
    )


class ErrorInfo(BaseModel):
    """Error info from ERROR-INFO (level 10)."""

    model_config = {"from_attributes": True}

    error_type: str = Field(
        max_length=1,
        description=(
            "Error type: V=Validation, P=Processing, D=Database, F=File, S=Security. "
            "COBOL: ERROR-TYPE PIC X(1). "
            "88-level values: V, P, D, F, S."
        ),
    )
    error_code: str = Field(
        max_length=4,
        description="Error code. COBOL: ERROR-CODE PIC X(4).",
    )
    error_text: str = Field(
        max_length=80,
        description="Error text. COBOL: ERROR-TEXT PIC X(80).",
    )

    @field_validator("error_type")
    @classmethod
    def validate_error_type(cls, v: str) -> str:
        valid = {"V", "P", "D", "F", "S"}
        if v.strip() and v.strip() not in valid:
            raise ValueError(f"error_type must be one of {valid}")
        return v


class SystemInfo(BaseModel):
    """System info from SYSTEM-INFO (level 10)."""

    model_config = {"from_attributes": True}

    system_code: str = Field(
        max_length=4,
        description="System code. COBOL: SYSTEM-CODE PIC X(4).",
    )
    system_msg: str = Field(
        max_length=80,
        description="System message. COBOL: SYSTEM-MSG PIC X(80).",
    )


class ReturnDetails(BaseModel):
    """Return details from RETURN-DETAILS (level 05)."""

    model_config = {"from_attributes": True}

    error_location: ErrorLocation = Field(
        description="Error location (ERROR-LOCATION).",
    )
    error_info: ErrorInfo = Field(description="Error info (ERROR-INFO).")
    system_info: SystemInfo = Field(description="System info (SYSTEM-INFO).")


class ReturnActions(BaseModel):
    """Return actions from RETURN-ACTIONS (level 05)."""

    model_config = {"from_attributes": True}

    action_flag: str = Field(
        max_length=1,
        description=(
            "Action flag: C=Continue, A=Abort, R=Retry. "
            "COBOL: ACTION-FLAG PIC X(1). "
            "88-level values: C, A, R."
        ),
    )
    retry_count: int = Field(
        description="Current retry count. COBOL: RETRY-COUNT PIC 9(2) COMP.",
    )
    max_retries: int = Field(
        default=3,
        description="Maximum retries. COBOL: MAX-RETRIES PIC 9(2) COMP VALUE 3.",
    )

    @field_validator("action_flag")
    @classmethod
    def validate_action_flag(cls, v: str) -> str:
        valid = {"C", "A", "R"}
        if v.strip() and v.strip() not in valid:
            raise ValueError(f"action_flag must be one of {valid}")
        return v


class ReturnHandling(BaseModel):
    """
    Return code handling -- maps to COBOL 01-level RETURN-HANDLING.

    Source: src/copybook/common/RETHND.cpy
    """

    model_config = {"from_attributes": True}

    return_status: ReturnStatus = Field(
        description="Return status (RETURN-STATUS).",
    )
    return_details: ReturnDetails = Field(
        description="Return details (RETURN-DETAILS).",
    )
    return_actions: ReturnActions = Field(
        description="Return actions (RETURN-ACTIONS).",
    )


class StandardErrorCodes(BaseModel):
    """
    Standard error codes -- maps to COBOL 01-level STD-ERROR-CODES.

    Source: src/copybook/common/RETHND.cpy
    """

    model_config = {"from_attributes": True}

    err_invalid_data: str = Field(default="E001", max_length=4, description="COBOL: ERR-INVALID-DATA PIC X(4) VALUE 'E001'.")
    err_not_found: str = Field(default="E002", max_length=4, description="COBOL: ERR-NOT-FOUND PIC X(4) VALUE 'E002'.")
    err_duplicate: str = Field(default="E003", max_length=4, description="COBOL: ERR-DUPLICATE PIC X(4) VALUE 'E003'.")
    err_file_error: str = Field(default="E004", max_length=4, description="COBOL: ERR-FILE-ERROR PIC X(4) VALUE 'E004'.")
    err_db_error: str = Field(default="E005", max_length=4, description="COBOL: ERR-DB-ERROR PIC X(4) VALUE 'E005'.")
    err_security: str = Field(default="E006", max_length=4, description="COBOL: ERR-SECURITY PIC X(4) VALUE 'E006'.")
    err_processing: str = Field(default="E007", max_length=4, description="COBOL: ERR-PROCESSING PIC X(4) VALUE 'E007'.")
    err_validation: str = Field(default="E008", max_length=4, description="COBOL: ERR-VALIDATION PIC X(4) VALUE 'E008'.")
    err_version: str = Field(default="E009", max_length=4, description="COBOL: ERR-VERSION PIC X(4) VALUE 'E009'.")
    err_timeout: str = Field(default="E010", max_length=4, description="COBOL: ERR-TIMEOUT PIC X(4) VALUE 'E010'.")
