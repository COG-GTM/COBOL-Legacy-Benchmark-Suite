package com.portfolio.validation;

import com.portfolio.model.ValidationConstants;
import com.portfolio.model.ValidationRequest;
import com.portfolio.model.ValidationResult;

import java.math.BigDecimal;

public class PortfolioValidator {
    
    public ValidationResult validate(ValidationRequest request) {
        return switch (request.getValidationType()) {
            case ID -> validateId(request.getInputValue());
            case ACCOUNT -> validateAccount(request.getInputValue());
            case TYPE -> validateType(request.getInputValue());
            case AMOUNT -> validateAmount(request.getInputValue());
        };
    }
    
    private ValidationResult validateId(String inputValue) {
        if (inputValue == null || inputValue.length() < 8) {
            return ValidationResult.error(
                ValidationConstants.ReturnCodes.INVALID_ID,
                ValidationConstants.ErrorMessages.INVALID_ID
            );
        }
        
        if (!inputValue.substring(0, 4).equals(ValidationConstants.Constants.ID_PREFIX)) {
            return ValidationResult.error(
                ValidationConstants.ReturnCodes.INVALID_ID,
                ValidationConstants.ErrorMessages.INVALID_ID
            );
        }
        
        String numericPart = inputValue.substring(4, 8);
        if (!isNumeric(numericPart)) {
            return ValidationResult.error(
                ValidationConstants.ReturnCodes.INVALID_ID,
                ValidationConstants.ErrorMessages.INVALID_ID
            );
        }
        
        return ValidationResult.success();
    }
    
    private ValidationResult validateAccount(String inputValue) {
        if (inputValue == null || !isNumeric(inputValue) || inputValue.equals("0000000000")) {
            return ValidationResult.error(
                ValidationConstants.ReturnCodes.INVALID_ACCT,
                ValidationConstants.ErrorMessages.INVALID_ACCT
            );
        }
        
        return ValidationResult.success();
    }
    
    private ValidationResult validateType(String inputValue) {
        if (inputValue == null) {
            return ValidationResult.error(
                ValidationConstants.ReturnCodes.INVALID_TYPE,
                ValidationConstants.ErrorMessages.INVALID_TYPE
            );
        }
        
        String type = inputValue.trim();
        if (!type.equals("STK") && !type.equals("BND") && 
            !type.equals("MMF") && !type.equals("ETF")) {
            return ValidationResult.error(
                ValidationConstants.ReturnCodes.INVALID_TYPE,
                ValidationConstants.ErrorMessages.INVALID_TYPE
            );
        }
        
        return ValidationResult.success();
    }
    
    private ValidationResult validateAmount(String inputValue) {
        if (inputValue == null) {
            return ValidationResult.error(
                ValidationConstants.ReturnCodes.INVALID_AMT,
                ValidationConstants.ErrorMessages.INVALID_AMT
            );
        }
        
        try {
            BigDecimal amount = new BigDecimal(inputValue.trim());
            
            if (amount.compareTo(ValidationConstants.Constants.MIN_AMOUNT) < 0 ||
                amount.compareTo(ValidationConstants.Constants.MAX_AMOUNT) > 0) {
                return ValidationResult.error(
                    ValidationConstants.ReturnCodes.INVALID_AMT,
                    ValidationConstants.ErrorMessages.INVALID_AMT
                );
            }
            
            return ValidationResult.success();
        } catch (NumberFormatException e) {
            return ValidationResult.error(
                ValidationConstants.ReturnCodes.INVALID_AMT,
                ValidationConstants.ErrorMessages.INVALID_AMT
            );
        }
    }
    
    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        
        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        
        return true;
    }
}
