package com.clbs.portfolio.web;

import com.clbs.portfolio.service.PortfolioValidator;
import com.clbs.portfolio.service.ValidationResult;
import com.clbs.portfolio.web.dto.ValidationRequest;
import com.clbs.portfolio.web.dto.ValidationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/validations")
@Tag(name = "Validation", description = "Field validation rules (PORTVALD)")
public class ValidationController {

    private final PortfolioValidator validator;

    public ValidationController(PortfolioValidator validator) {
        this.validator = validator;
    }

    @PostMapping
    @Operation(operationId = "validateField", summary = "Run a PORTVALD check; always 200 with the return code")
    public ValidationResponse validate(@Valid @RequestBody ValidationRequest request) {
        ValidationResult result = validator.validate(request.type().charAt(0), request.value());
        return new ValidationResponse(result.returnCode(), result.message(), result.valid());
    }
}
