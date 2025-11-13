package com.portfolio.common;

import com.portfolio.model.ErrorConstants;
import com.portfolio.model.ErrorRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ErrorProcessorTest {
    
    private static final String TEST_LOG_FILE = "test-error.log";
    private ErrorProcessor errorProcessor;
    
    @BeforeEach
    void setUp() {
        errorProcessor = new ErrorProcessor(TEST_LOG_FILE);
    }
    
    @AfterEach
    void tearDown() throws IOException {
        Path logPath = Paths.get(TEST_LOG_FILE);
        if (Files.exists(logPath)) {
            Files.delete(logPath);
        }
    }
    
    @Test
    void testProcessError_CreatesLogFile() {
        ErrorRequest request = new ErrorRequest(
            "TESTPROG",
            ErrorConstants.Categories.VALIDATION,
            "E001",
            ErrorConstants.ReturnCodes.ERROR,
            "Test error message",
            "Test error details"
        );
        
        int returnCode = errorProcessor.processError(request);
        
        assertEquals(ErrorConstants.ReturnCodes.ERROR, returnCode);
        assertTrue(Files.exists(Paths.get(TEST_LOG_FILE)));
    }
    
    @Test
    void testProcessError_WritesCorrectData() throws IOException {
        ErrorRequest request = new ErrorRequest(
            "TESTPROG",
            ErrorConstants.Categories.SYSTEM,
            "E002",
            ErrorConstants.ReturnCodes.SEVERE,
            "System error occurred",
            "Additional details about the error"
        );
        
        errorProcessor.processError(request);
        
        Path logPath = Paths.get(TEST_LOG_FILE);
        List<String> lines = Files.readAllLines(logPath);
        
        assertFalse(lines.isEmpty());
        String logLine = lines.get(0);
        
        assertTrue(logLine.contains("TESTPROG"));
        assertTrue(logLine.contains("SY"));
        assertTrue(logLine.contains("E002"));
        assertTrue(logLine.contains("System error occurred"));
    }
    
    @Test
    void testProcessError_MultipleErrors() throws IOException {
        ErrorRequest request1 = new ErrorRequest(
            "PROG1",
            ErrorConstants.Categories.VALIDATION,
            "E001",
            ErrorConstants.ReturnCodes.WARNING,
            "First error",
            "First details"
        );
        
        ErrorRequest request2 = new ErrorRequest(
            "PROG2",
            ErrorConstants.Categories.PROCESSING,
            "E002",
            ErrorConstants.ReturnCodes.ERROR,
            "Second error",
            "Second details"
        );
        
        errorProcessor.processError(request1);
        errorProcessor.processError(request2);
        
        Path logPath = Paths.get(TEST_LOG_FILE);
        List<String> lines = Files.readAllLines(logPath);
        
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).contains("PROG1"));
        assertTrue(lines.get(1).contains("PROG2"));
    }
    
    @Test
    void testProcessError_ReturnsSeverity() {
        ErrorRequest warningRequest = new ErrorRequest(
            "TESTPROG",
            ErrorConstants.Categories.VALIDATION,
            "W001",
            ErrorConstants.ReturnCodes.WARNING,
            "Warning message",
            "Warning details"
        );
        
        int returnCode = errorProcessor.processError(warningRequest);
        assertEquals(ErrorConstants.ReturnCodes.WARNING, returnCode);
    }
}
