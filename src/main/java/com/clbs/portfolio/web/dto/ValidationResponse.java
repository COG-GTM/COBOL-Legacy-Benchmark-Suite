package com.clbs.portfolio.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "PORTVALD result: 0=valid, 1=invalid ID or request type, 2=invalid account, 3=invalid type, 4=invalid amount.")
public record ValidationResponse(int returnCode, String message, boolean valid) {
}
