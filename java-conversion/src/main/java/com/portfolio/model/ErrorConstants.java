package com.portfolio.model;

public final class ErrorConstants {
    
    public static final class Categories {
        public static final String VSAM = "VS";
        public static final String VALIDATION = "VL";
        public static final String PROCESSING = "PR";
        public static final String SYSTEM = "SY";
        
        private Categories() {}
    }
    
    public static final class ReturnCodes {
        public static final int SUCCESS = 0;
        public static final int WARNING = 4;
        public static final int ERROR = 8;
        public static final int SEVERE = 12;
        public static final int TERMINAL = 16;
        
        private ReturnCodes() {}
    }
    
    public static final class VsamStatuses {
        public static final String SUCCESS = "00";
        public static final String DUPLICATE_KEY = "22";
        public static final String NOT_FOUND = "23";
        public static final String EOF = "10";
        
        private VsamStatuses() {}
    }
    
    public static final class VsamMessages {
        public static final String DUPLICATE_KEY = "Duplicate record key";
        public static final String NOT_FOUND = "Record not found";
        public static final String OTHER = "Unexpected VSAM error";
        
        private VsamMessages() {}
    }
    
    private ErrorConstants() {}
}
