package com.portfolio.validation;

import com.portfolio.model.ValidationConstants;
import com.portfolio.model.ValidationRequest;
import com.portfolio.model.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PortfolioValidatorTest {
    
    private PortfolioValidator validator;
    
    @BeforeEach
    void setUp() {
        validator = new PortfolioValidator();
    }
    
    @Test
    void testValidateId_ValidId() {
        ValidationRequest request = new ValidationRequest(
            ValidationRequest.ValidationType.ID, "PORT1234");
        ValidationResult result = validator.validate(request);
        
        assertTrue(result.isSuccess());
        assertEquals(ValidationConstants.ReturnCodes.SUCCESS, result.getReturnCode());
        assertEquals("", result.getErrorMessage());
    }
    
    @Test
    void testValidateId_InvalidPrefix() {
        ValidationRequest request = new ValidationRequest(
            ValidationRequest.ValidationType.ID, "ABCD1234");
        ValidationResult result = validator.validate(request);
        
        assertFalse(result.isSuccess());
        assertEquals(ValidationConstants.ReturnCodes.INVALID_ID, result.getReturnCode());
        assertEquals(ValidationConstants.ErrorMessages.INVALID_ID, result.getErrorMessage());
    }
    
    @Test
    void testValidateId_NonNumericSuffix() {
        ValidationRequest request = new ValidationRequest(
            ValidationRequest.ValidationType.ID, "PORTABCD");
        ValidationResult result = validator.validate(request);
        
        assertFalse(result.isSuccess());
        assertEquals(ValidationConstants.ReturnCodes.INVALID_ID, result.getReturnCode());
    }
    
    @Test
    void testValidateId_TooShort() {
        ValidationRequest request = new ValidationRequest(
            ValidationRequest.ValidationType.ID, "PORT12");
        ValidationResult result = validator.validate(request);
        
        assertFalse(result.isSuccess());
        assertEquals(ValidationConstants.ReturnCodes.INVALID_ID, result.getReturnCode());
    }
    
    @Test
    void testValidateAccount_ValidAccount() {
        ValidationRequest request = new ValidationRequest(
            ValidationRequest.ValidationType.ACCOUNT, "1234567890");
        ValidationResult result = validator.validate(request);
        
        assertTrue(result.isSuccess());
        assertEquals(ValidationConstants.ReturnCodes.SUCCESS, result.getReturnCode());
    }
    
    @Test
    void testValidateAccount_NonNumeric() {
        ValidationRequest request = new ValidationRequest(
            ValidationRequest.ValidationType.ACCOUNT, "12345ABC90");
        ValidationResult result = validator.validate(request);
        
        assertFalse(result.isSuccess());
        assertEquals(ValidationConstants.ReturnCodes.INVALID_ACCT, result.getReturnCode());
    }
    
    @Test
    void testValidateAccount_AllZeros() {
        ValidationRequest request = new ValidationRequest(
            ValidationRequest.ValidationType.ACCOUNT, "0000000000");
        ValidationResult result = validator.validate(request);
        
        assertFalse(result.isSuccess());
        assertEquals(ValidationConstants.ReturnCodes.INVALID_ACCT, result.getReturnCode());
    }
    
    @Test
    void testValidateType_ValidStock() {
        ValidationRequest request = new ValidationRequest(
            ValidationRequest.ValidationType.TYPE, "STK");
        ValidationResult result = validator.validate(request);
        
        assertTrue(result.isSuccess());
    }
    
    @Test
    void testValidateType_ValidBond() {
        ValidationRequest request = new ValidationRequest(
            ValidationRequest.ValidationType.TYPE, "BND");
        ValidationResult result = validator.validate(request);
        
        assertTrue(result.isSuccess());
    }
    
    @Test
    void testValidateType_ValidMMF() {
        ValidationRequest request = new ValidationRequest(
            ValidationRequest.ValidationType.TYPE, "MMF");
        ValidationResult result = validator.validate(request);
        
        assertTrue(result.isSuccess());
    }
    
    @Test
    void testValidateType_ValidETF() {
        ValidationRequest request = new ValidationRequest(
            ValidationRequest.ValidationType.TYPE, "ETF");
        ValidationResult result = validator.validate(request);
        
        assertTrue(result.isSuccess());
    }
    
    @Test
    void testValidateType_Invalid() {
        ValidationRequest request = new ValidationRequest(
            ValidationRequest.ValidationType.TYPE, "XYZ");
        ValidationResult result = validator.validate(request);
        
        assertFalse(result.isSuccess());
        assertEquals(ValidationConstants.ReturnCodes.INVALID_TYPE, result.getReturnCode());
    }
    
    @Test
    void testValidateAmount_ValidPositive() {
        ValidationRequest request = new ValidationRequest(
            ValidationRequest.ValidationType.AMOUNT, "1000.50");
        ValidationResult result = validator.validate(request);
        
        assertTrue(result.isSuccess());
    }
    
    @Test
    void testValidateAmount_ValidNegative() {
        ValidationRequest request = new ValidationRequest(
            ValidationRequest.ValidationType.AMOUNT, "-500.25");
        ValidationResult result = validator.validate(request);
        
        assertTrue(result.isSuccess());
    }
    
    @Test
    void testValidateAmount_TooLarge() {
        ValidationRequest request = new ValidationRequest(
            ValidationRequest.ValidationType.AMOUNT, "99999999999999.99");
        ValidationResult result = validator.validate(request);
        
        assertFalse(result.isSuccess());
        assertEquals(ValidationConstants.ReturnCodes.INVALID_AMT, result.getReturnCode());
    }
    
    @Test
    void testValidateAmount_TooSmall() {
        ValidationRequest request = new ValidationRequest(
            ValidationRequest.ValidationType.AMOUNT, "-99999999999999.99");
        ValidationResult result = validator.validate(request);
        
        assertFalse(result.isSuccess());
        assertEquals(ValidationConstants.ReturnCodes.INVALID_AMT, result.getReturnCode());
    }
    
    @Test
    void testValidateAmount_NonNumeric() {
        ValidationRequest request = new ValidationRequest(
            ValidationRequest.ValidationType.AMOUNT, "ABC");
        ValidationResult result = validator.validate(request);
        
        assertFalse(result.isSuccess());
        assertEquals(ValidationConstants.ReturnCodes.INVALID_AMT, result.getReturnCode());
    }
}
