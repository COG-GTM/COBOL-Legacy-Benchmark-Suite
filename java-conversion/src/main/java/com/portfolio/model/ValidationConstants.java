package com.portfolio.model;

import java.math.BigDecimal;

public final class ValidationConstants {
    
    public static final class ReturnCodes {
        public static final int SUCCESS = 0;
        public static final int INVALID_ID = 1;
        public static final int INVALID_ACCT = 2;
        public static final int INVALID_TYPE = 3;
        public static final int INVALID_AMT = 4;
        
        private ReturnCodes() {}
    }
    
    public static final class ErrorMessages {
        public static final String INVALID_ID = "Invalid Portfolio ID format";
        public static final String INVALID_ACCT = "Invalid Account Number format";
        public static final String INVALID_TYPE = "Invalid Investment Type";
        public static final String INVALID_AMT = "Amount outside valid range";
        
        private ErrorMessages() {}
    }
    
    public static final class Constants {
        public static final BigDecimal MIN_AMOUNT = new BigDecimal("-9999999999999.99");
        public static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999999.99");
        public static final String ID_PREFIX = "PORT";
        
        private Constants() {}
    }
    
    private ValidationConstants() {}
}
