"""
Pydantic v2 models for COBOL INQCOM copybook (Online Inquiry Communication Area).

Source: src/copybook/online/INQCOM.cpy
"""

from pydantic import BaseModel, Field, field_validator


class InquiryCommunicationArea(BaseModel):
    """
    Online Inquiry Communication Area -- maps to COBOL 01-level INQCOM-AREA.

    Source: src/copybook/online/INQCOM.cpy
    """

    model_config = {"from_attributes": True}

    inqcom_function: str = Field(
        max_length=4,
        description=(
            "Inquiry function: MENU=Menu, INQP=Portfolio Inquiry, "
            "INQH=History Inquiry, EXIT=Exit. "
            "COBOL: INQCOM-FUNCTION PIC X(4). "
            "88-level values: MENU, INQP, INQH, EXIT."
        ),
    )
    inqcom_account_no: str = Field(
        max_length=10,
        description="Account number. COBOL: INQCOM-ACCOUNT-NO PIC X(10).",
    )
    inqcom_response_code: int = Field(
        description="Response code. COBOL: INQCOM-RESPONSE-CODE PIC S9(8) COMP.",
    )
    inqcom_error_msg: str = Field(
        default="",
        max_length=80,
        description="Error message. COBOL: INQCOM-ERROR-MSG PIC X(80).",
    )

    @field_validator("inqcom_function")
    @classmethod
    def validate_function(cls, v: str) -> str:
        valid = {"MENU", "INQP", "INQH", "EXIT"}
        if v.strip() and v.strip() not in valid:
            raise ValueError(f"inqcom_function must be one of {valid}")
        return v
