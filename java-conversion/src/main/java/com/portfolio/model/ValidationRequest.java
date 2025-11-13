package com.portfolio.model;

public class ValidationRequest {
    
    public enum ValidationType {
        ID('I'),
        ACCOUNT('A'),
        TYPE('T'),
        AMOUNT('M');
        
        private final char code;
        
        ValidationType(char code) {
            this.code = code;
        }
        
        public char getCode() {
            return code;
        }
        
        public static ValidationType fromCode(char code) {
            for (ValidationType type : values()) {
                if (type.code == code) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Invalid validation type code: " + code);
        }
    }
    
    private final ValidationType validationType;
    private final String inputValue;
    
    public ValidationRequest(ValidationType validationType, String inputValue) {
        this.validationType = validationType;
        this.inputValue = inputValue;
    }
    
    public ValidationRequest(char validationTypeCode, String inputValue) {
        this(ValidationType.fromCode(validationTypeCode), inputValue);
    }
    
    public ValidationType getValidationType() {
        return validationType;
    }
    
    public String getInputValue() {
        return inputValue;
    }
}
