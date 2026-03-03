"""
Pydantic v2 models for COBOL RTNCODE copybook (Return Code Management).

Source: src/copybook/common/RTNCODE.cpy
"""

from pydantic import BaseModel, Field, field_validator


class ReturnCodesArea(BaseModel):
    """Codes area from RC-CODES-AREA (level 05)."""

    model_config = {"from_attributes": True}

    rc_current_code: int = Field(
        description="Current return code. COBOL: RC-CURRENT-CODE PIC S9(4) COMP.",
    )
    rc_highest_code: int = Field(
        description="Highest return code seen. COBOL: RC-HIGHEST-CODE PIC S9(4) COMP.",
    )
    rc_new_code: int = Field(
        description="New return code to set. COBOL: RC-NEW-CODE PIC S9(4) COMP.",
    )
    rc_status: str = Field(
        max_length=1,
        description=(
            "Return code status: S=Success, W=Warning, E=Error, F=Severe. "
            "COBOL: RC-STATUS PIC X. "
            "88-level values: S, W, E, F."
        ),
    )

    @field_validator("rc_status")
    @classmethod
    def validate_rc_status(cls, v: str) -> str:
        valid = {"S", "W", "E", "F"}
        if v.strip() and v.strip() not in valid:
            raise ValueError(f"rc_status must be one of {valid}")
        return v


class ReturnCodeAnalysisData(BaseModel):
    """Analysis data from RC-ANALYSIS-DATA (level 05)."""

    model_config = {"from_attributes": True}

    rc_start_time: str = Field(
        max_length=26,
        description="Analysis start time. COBOL: RC-START-TIME PIC X(26).",
    )
    rc_end_time: str = Field(
        max_length=26,
        description="Analysis end time. COBOL: RC-END-TIME PIC X(26).",
    )
    rc_total_codes: int = Field(
        description="Total codes processed. COBOL: RC-TOTAL-CODES PIC S9(8) COMP.",
    )
    rc_max_code: int = Field(
        description="Maximum code value. COBOL: RC-MAX-CODE PIC S9(4) COMP.",
    )
    rc_min_code: int = Field(
        description="Minimum code value. COBOL: RC-MIN-CODE PIC S9(4) COMP.",
    )


class ReturnData(BaseModel):
    """Return data from RC-RETURN-DATA (level 05)."""

    model_config = {"from_attributes": True}

    rc_return_value: int = Field(
        description="Return value. COBOL: RC-RETURN-VALUE PIC S9(4) COMP.",
    )
    rc_highest_return: int = Field(
        description="Highest return value. COBOL: RC-HIGHEST-RETURN PIC S9(4) COMP.",
    )
    rc_return_status: str = Field(
        max_length=1,
        description="Return status. COBOL: RC-RETURN-STATUS PIC X.",
    )


class ReturnCodeArea(BaseModel):
    """
    Return Code Management -- maps to COBOL 01-level RETURN-CODE-AREA.

    Source: src/copybook/common/RTNCODE.cpy
    """

    model_config = {"from_attributes": True}

    rc_request_type: str = Field(
        max_length=1,
        description=(
            "Request type: I=Initialize, S=Set Code, G=Get Code, L=Log Code, A=Analyze. "
            "COBOL: RC-REQUEST-TYPE PIC X. "
            "88-level values: I, S, G, L, A."
        ),
    )
    rc_program_id: str = Field(
        max_length=8,
        description="Program identifier. COBOL: RC-PROGRAM-ID PIC X(8).",
    )
    rc_codes_area: ReturnCodesArea = Field(
        description="Return codes area (RC-CODES-AREA).",
    )
    rc_message: str = Field(
        max_length=80,
        description="Return code message. COBOL: RC-MESSAGE PIC X(80).",
    )
    rc_response_code: int = Field(
        description="Response code. COBOL: RC-RESPONSE-CODE PIC S9(8) COMP.",
    )
    rc_analysis_data: ReturnCodeAnalysisData = Field(
        description="Analysis data (RC-ANALYSIS-DATA).",
    )
    rc_return_data: ReturnData = Field(
        description="Return data (RC-RETURN-DATA).",
    )

    @field_validator("rc_request_type")
    @classmethod
    def validate_request_type(cls, v: str) -> str:
        valid = {"I", "S", "G", "L", "A"}
        if v.strip() and v.strip() not in valid:
            raise ValueError(f"rc_request_type must be one of {valid}")
        return v
