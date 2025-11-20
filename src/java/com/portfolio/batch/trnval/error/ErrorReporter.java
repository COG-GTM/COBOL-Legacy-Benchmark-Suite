package com.portfolio.batch.trnval.error;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Error Reporter - Manages error logging and reporting
 * 
 * Corresponds to COBOL error handling patterns from ERRHAND copybook
 * and UTLVAL00 error reporting logic
 */
public class ErrorReporter {
    
    private final List<ValidationError> errors;
    private final Path errorReportPath;
    private BufferedWriter errorWriter;
    private int errorCount;
    private int warningCount;
    
    public ErrorReporter(Path errorReportPath) {
        this.errors = new ArrayList<>();
        this.errorReportPath = errorReportPath;
        this.errorCount = 0;
        this.warningCount = 0;
    }
    
    public void initialize() throws IOException {
        errorWriter = Files.newBufferedWriter(errorReportPath, 
                StandardOpenOption.CREATE, 
                StandardOpenOption.TRUNCATE_EXISTING);
        writeHeader();
    }
    
    private void writeHeader() throws IOException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        errorWriter.write("================================================================================\n");
        errorWriter.write("                    TRANSACTION VALIDATION ERROR REPORT\n");
        errorWriter.write("                           TRNVAL00 - Java Version\n");
        errorWriter.write("================================================================================\n");
        errorWriter.write("Report Generated: " + LocalDateTime.now().format(formatter) + "\n");
        errorWriter.write("================================================================================\n\n");
        errorWriter.flush();
    }
    
    public void addError(ValidationError error) {
        errors.add(error);
        if (error.isError()) {
            errorCount++;
        } else if (error.isWarning()) {
            warningCount++;
        }
        
        try {
            if (errorWriter != null) {
                errorWriter.write(error.formatErrorLine() + "\n");
                errorWriter.flush();
            }
        } catch (IOException e) {
            System.err.println("Failed to write error to report: " + e.getMessage());
        }
    }
    
    public void writeFooter(int totalRecords, int validRecords) throws IOException {
        if (errorWriter != null) {
            errorWriter.write("\n================================================================================\n");
            errorWriter.write("                           VALIDATION SUMMARY\n");
            errorWriter.write("================================================================================\n");
            errorWriter.write(String.format("Total Records Processed:  %,10d\n", totalRecords));
            errorWriter.write(String.format("Valid Records:            %,10d\n", validRecords));
            errorWriter.write(String.format("Records with Errors:      %,10d\n", errorCount));
            errorWriter.write(String.format("Records with Warnings:    %,10d\n", warningCount));
            errorWriter.write("================================================================================\n");
            errorWriter.flush();
        }
    }
    
    public void close() throws IOException {
        if (errorWriter != null) {
            errorWriter.close();
        }
    }
    
    public List<ValidationError> getErrors() {
        return new ArrayList<>(errors);
    }
    
    public int getErrorCount() {
        return errorCount;
    }
    
    public int getWarningCount() {
        return warningCount;
    }
    
    public boolean hasErrors() {
        return errorCount > 0;
    }
    
    public boolean hasWarnings() {
        return warningCount > 0;
    }
    
    public int determineReturnCode() {
        if (errorCount > 0) {
            return 8;
        } else if (warningCount > 0) {
            return 4;
        } else {
            return 0;
        }
    }
}
