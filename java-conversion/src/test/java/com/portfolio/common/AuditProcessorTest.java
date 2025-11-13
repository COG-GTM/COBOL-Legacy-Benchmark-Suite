package com.portfolio.common;

import com.portfolio.model.AuditRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuditProcessorTest {
    
    private static final String TEST_AUDIT_FILE = "test-audit.log";
    private AuditProcessor auditProcessor;
    
    @BeforeEach
    void setUp() {
        auditProcessor = new AuditProcessor(TEST_AUDIT_FILE);
    }
    
    @AfterEach
    void tearDown() throws IOException {
        Path auditPath = Paths.get(TEST_AUDIT_FILE);
        if (Files.exists(auditPath)) {
            Files.delete(auditPath);
        }
    }
    
    @Test
    void testProcessAudit_CreatesAuditFile() {
        AuditRequest request = new AuditRequest(
            "SYS001",
            "USER123",
            "TESTPROG",
            "TERM001",
            AuditRequest.AuditType.TRANSACTION,
            AuditRequest.AuditAction.CREATE,
            AuditRequest.AuditStatus.SUCCESS,
            "PORT1234",
            "1234567890",
            "Before data",
            "After data",
            "Test audit message"
        );
        
        int returnCode = auditProcessor.processAudit(request);
        
        assertEquals(0, returnCode);
        assertTrue(Files.exists(Paths.get(TEST_AUDIT_FILE)));
    }
    
    @Test
    void testProcessAudit_WritesCorrectData() throws IOException {
        AuditRequest request = new AuditRequest(
            "SYS001",
            "USER123",
            "PORTUPD",
            "TERM001",
            AuditRequest.AuditType.TRANSACTION,
            AuditRequest.AuditAction.UPDATE,
            AuditRequest.AuditStatus.SUCCESS,
            "PORT5678",
            "9876543210",
            "Old portfolio data",
            "New portfolio data",
            "Portfolio updated successfully"
        );
        
        auditProcessor.processAudit(request);
        
        Path auditPath = Paths.get(TEST_AUDIT_FILE);
        List<String> lines = Files.readAllLines(auditPath);
        
        assertFalse(lines.isEmpty());
        String auditLine = lines.get(0);
        
        assertTrue(auditLine.contains("SYS001"));
        assertTrue(auditLine.contains("USER123"));
        assertTrue(auditLine.contains("PORTUPD"));
        assertTrue(auditLine.contains("PORT5678"));
        assertTrue(auditLine.contains("9876543210"));
    }
    
    @Test
    void testProcessAudit_MultipleRecords() throws IOException {
        AuditRequest request1 = new AuditRequest(
            "SYS001",
            "USER001",
            "PROG1",
            "TERM001",
            AuditRequest.AuditType.USER_ACTION,
            AuditRequest.AuditAction.LOGIN,
            AuditRequest.AuditStatus.SUCCESS,
            "",
            "",
            "",
            "",
            "User logged in"
        );
        
        AuditRequest request2 = new AuditRequest(
            "SYS001",
            "USER001",
            "PROG2",
            "TERM001",
            AuditRequest.AuditType.TRANSACTION,
            AuditRequest.AuditAction.CREATE,
            AuditRequest.AuditStatus.SUCCESS,
            "PORT1111",
            "1111111111",
            "",
            "New portfolio created",
            "Portfolio creation"
        );
        
        auditProcessor.processAudit(request1);
        auditProcessor.processAudit(request2);
        
        Path auditPath = Paths.get(TEST_AUDIT_FILE);
        List<String> lines = Files.readAllLines(auditPath);
        
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).contains("LOGIN"));
        assertTrue(lines.get(1).contains("CREATE"));
    }
    
    @Test
    void testProcessAudit_DifferentAuditTypes() throws IOException {
        AuditRequest transactionRequest = new AuditRequest(
            "SYS001",
            "USER001",
            "TRNPROG",
            "TERM001",
            AuditRequest.AuditType.TRANSACTION,
            AuditRequest.AuditAction.CREATE,
            AuditRequest.AuditStatus.SUCCESS,
            "PORT1234",
            "1234567890",
            "",
            "",
            "Transaction audit"
        );
        
        AuditRequest systemRequest = new AuditRequest(
            "SYS001",
            "SYSTEM",
            "SYSPROG",
            "CONSOLE",
            AuditRequest.AuditType.SYSTEM_EVENT,
            AuditRequest.AuditAction.STARTUP,
            AuditRequest.AuditStatus.SUCCESS,
            "",
            "",
            "",
            "",
            "System startup"
        );
        
        auditProcessor.processAudit(transactionRequest);
        auditProcessor.processAudit(systemRequest);
        
        Path auditPath = Paths.get(TEST_AUDIT_FILE);
        List<String> lines = Files.readAllLines(auditPath);
        
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).contains("TRAN"));
        assertTrue(lines.get(1).contains("SYST"));
    }
    
    @Test
    void testProcessAudit_FailureStatus() throws IOException {
        AuditRequest request = new AuditRequest(
            "SYS001",
            "USER001",
            "TESTPROG",
            "TERM001",
            AuditRequest.AuditType.TRANSACTION,
            AuditRequest.AuditAction.DELETE,
            AuditRequest.AuditStatus.FAILURE,
            "PORT9999",
            "9999999999",
            "Existing data",
            "",
            "Delete operation failed"
        );
        
        int returnCode = auditProcessor.processAudit(request);
        
        assertEquals(0, returnCode);
        
        Path auditPath = Paths.get(TEST_AUDIT_FILE);
        List<String> lines = Files.readAllLines(auditPath);
        
        assertTrue(lines.get(0).contains("FAIL"));
    }
}
