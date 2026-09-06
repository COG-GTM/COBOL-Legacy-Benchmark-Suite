package com.cog.portfolio.dto;

import com.cog.portfolio.common.CodedValue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** INQCOM.cpy INQCOM-AREA (CICS COMMAREA of INQONLN). */
public record InquiryRequest(
        Function function,
        @NotBlank @Pattern(regexp = "\\d{10}", message = "Account number must be 10 digits") String accountNo) {

    /** INQCOM-FUNCTION level-88 values. */
    public enum Function implements CodedValue {
        MENU("MENU"), PORTFOLIO("INQP"), HISTORY("INQH"), EXIT("EXIT");

        private final String code;

        Function(String code) {
            this.code = code;
        }

        @Override
        public String code() {
            return code;
        }

        public static Function fromCode(String code) {
            return CodedValue.fromCode(values(), code);
        }
    }
}
