import {
  createPortfolioSchema,
  updatePortfolioSchema,
  deleteReasonCodeSchema,
} from "@/lib/validations/portfolio";

describe("Portfolio Validation Schemas — ported from PORTVALD.cbl + PORTVAL.cpy", () => {
  describe("createPortfolioSchema", () => {
    const validInput = {
      portfolio_id: "PORT0001",
      account_no: "1234567890",
      client_name: "John Doe",
      client_type: "I" as const,
      updated_by: "WEBUSER",
    };

    it("accepts valid input", () => {
      const result = createPortfolioSchema.safeParse(validInput);
      expect(result.success).toBe(true);
    });

    // Portfolio ID validation (PORTVALD.cbl 1000-VALIDATE-ID)
    it("rejects portfolio ID not starting with PORT", () => {
      const result = createPortfolioSchema.safeParse({
        ...validInput,
        portfolio_id: "ACCT0001",
      });
      expect(result.success).toBe(false);
    });

    it("rejects portfolio ID without 4 trailing digits", () => {
      const result = createPortfolioSchema.safeParse({
        ...validInput,
        portfolio_id: "PORTABCD",
      });
      expect(result.success).toBe(false);
    });

    it("rejects portfolio ID with wrong length", () => {
      const result = createPortfolioSchema.safeParse({
        ...validInput,
        portfolio_id: "PORT00001",
      });
      expect(result.success).toBe(false);
    });

    // Account number validation (PORTVALD.cbl 2000-VALIDATE-ACCOUNT)
    it("rejects non-numeric account number", () => {
      const result = createPortfolioSchema.safeParse({
        ...validInput,
        account_no: "ABCDEFGHIJ",
      });
      expect(result.success).toBe(false);
    });

    it("rejects all-zeros account number", () => {
      const result = createPortfolioSchema.safeParse({
        ...validInput,
        account_no: "0000000000",
      });
      expect(result.success).toBe(false);
    });

    it("rejects account number with wrong length", () => {
      const result = createPortfolioSchema.safeParse({
        ...validInput,
        account_no: "12345",
      });
      expect(result.success).toBe(false);
    });

    // Client type validation (PORTFLIO.cpy lines 17-20)
    it("accepts valid client types I, C, T", () => {
      for (const ct of ["I", "C", "T"] as const) {
        const result = createPortfolioSchema.safeParse({
          ...validInput,
          client_type: ct,
        });
        expect(result.success).toBe(true);
      }
    });

    it("rejects invalid client type", () => {
      const result = createPortfolioSchema.safeParse({
        ...validInput,
        client_type: "X",
      });
      expect(result.success).toBe(false);
    });

    // Status validation (PORTFLIO.cpy lines 24-27)
    it("accepts valid statuses A, C, S", () => {
      for (const s of ["A", "C", "S"] as const) {
        const result = createPortfolioSchema.safeParse({
          ...validInput,
          status: s,
        });
        expect(result.success).toBe(true);
      }
    });

    it("rejects invalid status", () => {
      const result = createPortfolioSchema.safeParse({
        ...validInput,
        status: "X",
      });
      expect(result.success).toBe(false);
    });

    // Amount validation (PORTVAL.cpy lines 35-36)
    it("accepts valid amounts", () => {
      const result = createPortfolioSchema.safeParse({
        ...validInput,
        total_value: "1000.50",
        cash_balance: "-500.25",
      });
      expect(result.success).toBe(true);
    });

    it("rejects amounts outside valid range", () => {
      const result = createPortfolioSchema.safeParse({
        ...validInput,
        total_value: "99999999999999.99",
      });
      expect(result.success).toBe(false);
    });

    // Client name validation (PORTMSTR.cbl line 149)
    it("rejects empty client name", () => {
      const result = createPortfolioSchema.safeParse({
        ...validInput,
        client_name: "",
      });
      expect(result.success).toBe(false);
    });

    it("rejects client name longer than 30 characters", () => {
      const result = createPortfolioSchema.safeParse({
        ...validInput,
        client_name: "A".repeat(31),
      });
      expect(result.success).toBe(false);
    });
  });

  describe("updatePortfolioSchema", () => {
    it("accepts partial updates with portfolio_id and updated_by", () => {
      const result = updatePortfolioSchema.safeParse({
        portfolio_id: "PORT0001",
        client_name: "Jane Doe",
        updated_by: "WEBUSER",
      });
      expect(result.success).toBe(true);
    });

    it("validates portfolio_id format on update", () => {
      const result = updatePortfolioSchema.safeParse({
        portfolio_id: "INVALID",
        updated_by: "WEBUSER",
      });
      expect(result.success).toBe(false);
    });
  });

  // Delete reason code validation (PORTDEL.cbl lines 50-52)
  describe("deleteReasonCodeSchema", () => {
    it("accepts valid reason codes 01, 02, 03", () => {
      for (const code of ["01", "02", "03"] as const) {
        const result = deleteReasonCodeSchema.safeParse(code);
        expect(result.success).toBe(true);
      }
    });

    it("rejects invalid reason codes", () => {
      const result = deleteReasonCodeSchema.safeParse("04");
      expect(result.success).toBe(false);
    });
  });
});
