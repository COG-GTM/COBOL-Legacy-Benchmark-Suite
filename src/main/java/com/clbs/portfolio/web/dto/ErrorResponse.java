package com.clbs.portfolio.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "Uniform error body. code is one of the RETHND standard error codes E001-E010.")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(OffsetDateTime timestamp, int status, String error, String code, String message,
                            String path, List<FieldErrorDetail> details) {

    public record FieldErrorDetail(String field, String message) {
    }
}
