/**
 * Unit tests for Portfolio Validation Service
 * Covers all validation rules from PORTVALD.cbl:
 *  - Portfolio ID format (PORTnnnn)
 *  - Account number (10 numeric digits, non-zero)
 *  - Investment type (STK, BND, MMF, ETF)
 *  - Amount range validation
 */

import { describe, it, expect } from "vitest";
import {
  validatePortfolioId,
  validateAccountNumber,
  validateInvestmentType,
  validateAmount,
  validate,
  ValidationCode,
} from "@/services/portfolio/validation";

describe("validatePortfolioId (1000-VALIDATE-ID)", () => {
  it("accepts valid portfolio ID PORT0001", () => {
    const result = validatePortfolioId("PORT0001");
    expect(result.code).toBe(ValidationCode.SUCCESS);
    expect(result.message).toBe("");
  });

  it("accepts PORT9999", () => {
    expect(validatePortfolioId("PORT9999").code).toBe(ValidationCode.SUCCESS);
  });

  it("accepts PORT0000", () => {
    expect(validatePortfolioId("PORT0000").code).toBe(ValidationCode.SUCCESS);
  });

  it("rejects ID not starting with PORT", () => {
    const result = validatePortfolioId("ACCT0001");
    expect(result.code).toBe(ValidationCode.INVALID_ID);
    expect(result.message).toBe("Invalid Portfolio ID format");
  });

  it("rejects ID with lowercase prefix", () => {
    expect(validatePortfolioId("port0001").code).toBe(ValidationCode.INVALID_ID);
  });

  it("rejects ID with non-numeric suffix", () => {
    expect(validatePortfolioId("PORTABCD").code).toBe(ValidationCode.INVALID_ID);
  });

  it("rejects ID with mixed alphanumeric suffix", () => {
    expect(validatePortfolioId("PORT00A1").code).toBe(ValidationCode.INVALID_ID);
  });

  it("rejects ID that is too short", () => {
    expect(validatePortfolioId("PORT01").code).toBe(ValidationCode.INVALID_ID);
  });

  it("rejects empty string", () => {
    expect(validatePortfolioId("").code).toBe(ValidationCode.INVALID_ID);
  });

  it("rejects ID with only prefix", () => {
    expect(validatePortfolioId("PORT").code).toBe(ValidationCode.INVALID_ID);
  });
});

describe("validateAccountNumber (2000-VALIDATE-ACCOUNT)", () => {
  it("accepts valid 10-digit account number", () => {
    const result = validateAccountNumber("1234567890");
    expect(result.code).toBe(ValidationCode.SUCCESS);
  });

  it("accepts account starting with zero", () => {
    expect(validateAccountNumber("0123456789").code).toBe(ValidationCode.SUCCESS);
  });

  it("rejects all-zero account number", () => {
    const result = validateAccountNumber("0000000000");
    expect(result.code).toBe(ValidationCode.INVALID_ACCOUNT);
    expect(result.message).toBe("Invalid Account Number format");
  });

  it("rejects account shorter than 10 digits", () => {
    expect(validateAccountNumber("123456789").code).toBe(
      ValidationCode.INVALID_ACCOUNT,
    );
  });

  it("rejects account longer than 10 digits", () => {
    expect(validateAccountNumber("12345678901").code).toBe(
      ValidationCode.INVALID_ACCOUNT,
    );
  });

  it("rejects account with alpha characters", () => {
    expect(validateAccountNumber("12345ABCDE").code).toBe(
      ValidationCode.INVALID_ACCOUNT,
    );
  });

  it("rejects account with spaces", () => {
    expect(validateAccountNumber("12345 6789").code).toBe(
      ValidationCode.INVALID_ACCOUNT,
    );
  });

  it("rejects empty string", () => {
    expect(validateAccountNumber("").code).toBe(ValidationCode.INVALID_ACCOUNT);
  });
});

describe("validateInvestmentType (3000-VALIDATE-TYPE)", () => {
  it.each(["STK", "BND", "MMF", "ETF"] as const)(
    "accepts valid type %s",
    (type) => {
      expect(validateInvestmentType(type).code).toBe(ValidationCode.SUCCESS);
    },
  );

  it("rejects unknown type", () => {
    const result = validateInvestmentType("OPT");
    expect(result.code).toBe(ValidationCode.INVALID_TYPE);
    expect(result.message).toBe("Invalid Investment Type");
  });

  it("rejects empty string", () => {
    expect(validateInvestmentType("").code).toBe(ValidationCode.INVALID_TYPE);
  });

  it("rejects lowercase valid type", () => {
    expect(validateInvestmentType("stk").code).toBe(ValidationCode.INVALID_TYPE);
  });
});

describe("validateAmount (4000-VALIDATE-AMOUNT)", () => {
  it("accepts zero", () => {
    expect(validateAmount("0").code).toBe(ValidationCode.SUCCESS);
  });

  it("accepts positive amount within range", () => {
    expect(validateAmount("100000.50").code).toBe(ValidationCode.SUCCESS);
  });

  it("accepts negative amount within range", () => {
    expect(validateAmount("-5000.00").code).toBe(ValidationCode.SUCCESS);
  });

  it("accepts maximum boundary value", () => {
    expect(validateAmount("9999999999999.99").code).toBe(ValidationCode.SUCCESS);
  });

  it("accepts minimum boundary value", () => {
    expect(validateAmount("-9999999999999.99").code).toBe(ValidationCode.SUCCESS);
  });

  it("rejects amount exceeding maximum", () => {
    const result = validateAmount("10000000000000.00");
    expect(result.code).toBe(ValidationCode.INVALID_AMOUNT);
    expect(result.message).toBe("Amount outside valid range");
  });

  it("rejects amount below minimum", () => {
    expect(validateAmount("-10000000000000.00").code).toBe(
      ValidationCode.INVALID_AMOUNT,
    );
  });

  it("accepts numeric input", () => {
    expect(validateAmount(42.5).code).toBe(ValidationCode.SUCCESS);
  });
});

describe("validate (top-level dispatcher)", () => {
  it("dispatches I to portfolio ID validation", () => {
    expect(validate("I", "PORT0001").code).toBe(ValidationCode.SUCCESS);
    expect(validate("I", "BADID").code).toBe(ValidationCode.INVALID_ID);
  });

  it("dispatches A to account number validation", () => {
    expect(validate("A", "1234567890").code).toBe(ValidationCode.SUCCESS);
    expect(validate("A", "bad").code).toBe(ValidationCode.INVALID_ACCOUNT);
  });

  it("dispatches T to investment type validation", () => {
    expect(validate("T", "STK").code).toBe(ValidationCode.SUCCESS);
    expect(validate("T", "XXX").code).toBe(ValidationCode.INVALID_TYPE);
  });

  it("dispatches M to amount validation", () => {
    expect(validate("M", "100.00").code).toBe(ValidationCode.SUCCESS);
    expect(validate("M", "99999999999999").code).toBe(ValidationCode.INVALID_AMOUNT);
  });
});
