package com.clbs.portfolio.web.dto;

import com.clbs.portfolio.service.UpdateAction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "PORTUPDT update: STATUS stores the first character, NAME the first 30, VALUE a decimal amount.")
public record PortfolioUpdateRequest(@NotNull UpdateAction action, @NotNull @Size(max = 50) String newValue) {
}
