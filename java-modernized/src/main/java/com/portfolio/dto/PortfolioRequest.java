package com.portfolio.dto;

import com.portfolio.validation.ValidPortfolioId;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for portfolio create/update operations.
 * Validation rules translated from PORTVALD.cbl paragraphs.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioRequest {

    /** Validated by 1000-VALIDATE-ID: must start with 'PORT' followed by 4 digits. */
    @ValidPortfolioId
    private String portId;

    /** Validated by 2000-VALIDATE-ACCOUNT: must be 10 numeric digits, non-zero. */
    @NotBlank(message = "Account number is required")
    @Pattern(regexp = "\\d{10}", message = "Invalid Account Number format")
    private String accountNo;

    /** Validated by 2100-VALIDATE-PORTFOLIO: PORT-NAME must not be SPACES. */
    @NotBlank(message = "Portfolio Name is required")
    @Size(max = 30, message = "Client name must not exceed 30 characters")
    private String clientName;

    /** Validated by PORTFLIO.cpy 88-levels: I=Individual, C=Corporate, T=Trust. */
    @NotBlank(message = "Client type is required")
    @Pattern(regexp = "[ICT]", message = "Client type must be I (Individual), C (Corporate), or T (Trust)")
    private String clientType;

    /** Validated by 2100-VALIDATE-PORTFOLIO: must be A, I, or C. Maps to PORTMSTR status check. */
    @NotBlank(message = "Status is required")
    @Pattern(regexp = "[ACS]", message = "Invalid Portfolio Status — must be A (Active), C (Closed), or S (Suspended)")
    private String status;

    /** PORT-TOTAL-VALUE — PIC S9(13)V99 COMP-3. Validated by 4000-VALIDATE-AMOUNT range check. */
    @DecimalMin(value = "0.00", message = "Total value must not be negative")
    private BigDecimal totalValue;

    /** PORT-CASH-BALANCE — PIC S9(13)V99 COMP-3. */
    @DecimalMin(value = "0.00", message = "Cash balance must not be negative")
    private BigDecimal cashBalance;
}
