package com.portfolio.common;

import com.portfolio.model.AuditRequest;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AuditProcessor {
    
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
    
    private final String auditFilePath;
    
    public AuditProcessor(String auditFilePath) {
        this.auditFilePath = auditFilePath;
    }
    
    public int processAudit(AuditRequest request) {
        try {
            initialize();
            String auditRecord = buildAuditRecord(request);
            writeToAuditFile(auditRecord);
            return 0;
        } catch (IOException e) {
            System.err.println("Error opening audit file: " + e.getMessage());
            return 8;
        }
    }
    
    private void initialize() throws IOException {
        Path auditPath = Paths.get(auditFilePath);
        if (!Files.exists(auditPath)) {
            Path parent = auditPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.createFile(auditPath);
        }
    }
    
    private String buildAuditRecord(AuditRequest request) {
        LocalDateTime timestamp = LocalDateTime.now();
        String formattedTimestamp = timestamp.format(TIMESTAMP_FORMATTER);
        
        return String.format("%-26s%-8s%-8s%-8s%-8s%-4s%-8s%-4s%-8s%-10s%-100s%-100s%-100s",
            formattedTimestamp,
            truncateOrPad(request.getSystemId(), 8),
            truncateOrPad(request.getUserId(), 8),
            truncateOrPad(request.getProgram(), 8),
            truncateOrPad(request.getTerminal(), 8),
            truncateOrPad(request.getType().getCode(), 4),
            truncateOrPad(request.getAction().getCode(), 8),
            truncateOrPad(request.getStatus().getCode(), 4),
            truncateOrPad(request.getPortfolioId(), 8),
            truncateOrPad(request.getAccountNo(), 10),
            truncateOrPad(request.getBeforeImage(), 100),
            truncateOrPad(request.getAfterImage(), 100),
            truncateOrPad(request.getMessage(), 100)
        );
    }
    
    private void writeToAuditFile(String record) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(auditFilePath, true))) {
            writer.write(record);
            writer.newLine();
        }
    }
    
    private String truncateOrPad(String value, int length) {
        if (value == null) {
            return " ".repeat(length);
        }
        if (value.length() > length) {
            return value.substring(0, length);
        }
        return String.format("%-" + length + "s", value);
    }
}
