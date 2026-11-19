package com.clbs.portfolio.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "PORTVALD request. type: I=portfolio ID, A=account number, T=investment type, M=amount.")
public record ValidationRequest(@NotNull @Size(min = 1, max = 1) String type, @Size(max = 50) String value) {
}
