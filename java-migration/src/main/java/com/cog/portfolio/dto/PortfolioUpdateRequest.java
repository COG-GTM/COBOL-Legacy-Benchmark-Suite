package com.cog.portfolio.dto;

import com.cog.portfolio.common.CodedValue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** PORTUPDT.cbl UPDATE-RECORD: key + update type (S/N/V) + new value. */
public record PortfolioUpdateRequest(
        @NotBlank String portfolioId,
        @NotBlank String accountNo,
        @NotNull UpdateType updateType,
        @NotBlank String newValue) {

    /** UPDT-TYPE level-88 values. */
    public enum UpdateType implements CodedValue {
        STATUS("S"), NAME("N"), VALUE("V");

        private final String code;

        UpdateType(String code) {
            this.code = code;
        }

        @Override
        public String code() {
            return code;
        }
    }
}
