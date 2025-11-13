package com.portfolio.common;

import com.portfolio.model.ErrorRequest;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ErrorProcessor {
    
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
    
    private final String logFilePath;
    
    public ErrorProcessor(String logFilePath) {
        this.logFilePath = logFilePath;
    }
    
    public int processError(ErrorRequest request) {
        try {
            initialize();
            String formattedMessage = buildErrorMessage(request);
            writeToLog(formattedMessage);
            displayError(request);
            return request.getSeverity();
        } catch (IOException e) {
            System.err.println("Error processing error log: " + e.getMessage());
            return request.getSeverity();
        }
    }
    
    private void initialize() throws IOException {
        Path logPath = Paths.get(logFilePath);
        if (!Files.exists(logPath)) {
            Path parent = logPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.createFile(logPath);
        }
    }
    
    private String buildErrorMessage(ErrorRequest request) {
        LocalDateTime timestamp = LocalDateTime.now();
        String formattedTimestamp = timestamp.format(TIMESTAMP_FORMATTER);
        
        return String.format("%-26s%-8s%-2s%-4s%04d%-80s%-256s",
            formattedTimestamp,
            truncateOrPad(request.getProgramId(), 8),
            truncateOrPad(request.getCategory(), 2),
            truncateOrPad(request.getErrorCode(), 4),
            request.getSeverity(),
            truncateOrPad(request.getErrorText(), 80),
            truncateOrPad(request.getErrorDetails(), 256)
        );
    }
    
    private void writeToLog(String message) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(logFilePath, true))) {
            writer.write(message);
            writer.newLine();
        }
    }
    
    private void displayError(ErrorRequest request) {
        System.out.println("====================================================");
        System.out.println("ERROR DETECTED: " + LocalDateTime.now().format(TIMESTAMP_FORMATTER));
        System.out.println("PROGRAM:       " + request.getProgramId());
        System.out.println("CATEGORY:      " + request.getCategory());
        System.out.println("CODE:          " + request.getErrorCode());
        System.out.println("SEVERITY:      " + request.getSeverity());
        System.out.println("MESSAGE:       " + request.getErrorText());
        System.out.println("DETAILS:       " + request.getErrorDetails());
        System.out.println("====================================================");
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
