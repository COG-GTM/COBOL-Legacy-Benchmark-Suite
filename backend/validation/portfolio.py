import re


def validate_account_number(account_number: str) -> tuple[bool, str]:
    """Validate account number: must be 10 numeric digits, not all zeros."""
    if not account_number or len(account_number) != 10:
        return False, "Account number must be exactly 10 digits"
    if not re.match(r"^\d{10}$", account_number):
        return False, "Account number must contain only numeric characters"
    if account_number == "0000000000":
        return False, "Account number cannot be all zeros"
    return True, "Valid account number"
