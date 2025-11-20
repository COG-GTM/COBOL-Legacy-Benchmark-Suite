package com.portfolio.batch.trnval;

import com.portfolio.batch.trnval.error.ErrorReporter;
import com.portfolio.batch.trnval.error.ValidationError;
import com.portfolio.batch.trnval.model.TransactionRecord;
import com.portfolio.batch.trnval.processor.TransactionFileProcessor;
import com.portfolio.batch.trnval.validation.TransactionValidator;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * TRNVAL00 - Transaction Validation Batch Program
 * 
 * Java 17 conversion of COBOL TRNVAL00 program
 * 
 * COBOL Program Structure:
 * ========================
 * IDENTIFICATION DIVISION.
 *     PROGRAM-ID. TRNVAL00.
 * 
 * ENVIRONMENT DIVISION.
 *     INPUT-OUTPUT SECTION.
 *         SELECT TRANFILE ASSIGN TO TRANFILE
 *             ORGANIZATION IS SEQUENTIAL
 *             ACCESS MODE IS SEQUENTIAL
 *             FILE STATUS IS WS-TRAN-STATUS.
 *         SELECT VALIDFILE ASSIGN TO VALIDFILE
 *             ORGANIZATION IS SEQUENTIAL
 *             FILE STATUS IS WS-VALID-STATUS.
 *         SELECT ERRORFILE ASSIGN TO ERRORFILE
 *             ORGANIZATION IS SEQUENTIAL
 *             FILE STATUS IS WS-ERROR-STATUS.
 * 
 * DATA DIVISION.
 *     FILE SECTION.
 *         COPY TRNREC.
 *     WORKING-STORAGE SECTION.
 *         COPY ERRHAND.
 *         COPY RTNCODE.
 * 
 * PROCEDURE DIVISION.
 *     0000-MAIN.
 *         PERFORM 1000-INITIALIZE
 *         PERFORM 2000-PROCESS
 *         PERFORM 3000-CLEANUP
 *         GOBACK.
 * 
 * Functionality:
 * ==============
 * 1. Sequential File Processing - Processes transaction files sequentially
 * 2. Validation Logic:
 *    - Portfolio ID validation (8 alphanumeric characters)
 *    - Investment ID validation (10 alphanumeric characters)
 *    - Transaction type validation (BU, SL, TR, FE)
 *    - Amount range checks (-99999999.99 to 99999999.99)
 *    - Data integrity verification
 *    - Business rule validation:
 *      * Share Quantity must not be zero for BUY/SELL
 *      * Amount must be non-zero for FEE
 *      * Price must be greater than zero for BUY/SELL
 *      * Transaction Date must not be future date
 * 3. Error Handling - Uses standardized error processing patterns
 * 4. Return Codes:
 *    - 0: Successful completion
 *    - 4: Warning, processing complete
 *    - 8: Errors, processing complete
 *    - 12: Critical error, abend
 * 
 * Usage:
 * ======
 * java TRNVAL00 <input-file> <output-file> <error-report>
 * 
 * Example:
 * java TRNVAL00 transactions.dat valid_transactions.dat error_report.txt
 * 
 * Input File Format:
 * ==================
 * Fixed-format records (89 characters minimum):
 * Positions 1-8:   Transaction Date (YYYYMMDD)
 * Positions 9-14:  Transaction Time (HHMMSS)
 * Positions 15-22: Portfolio ID (8 chars)
 * Positions 23-28: Sequence Number (6 digits)
 * Positions 29-38: Investment ID (10 chars)
 * Positions 39-40: Transaction Type (BU/SL/TR/FE)
 * Positions 41-55: Quantity (15 chars, decimal)
 * Positions 56-70: Price (15 chars, decimal)
 * Positions 71-85: Amount (15 chars, decimal)
 * Positions 86-88: Currency (3 chars)
 * Position 89:     Status (P/D/F/R)
 * 
 * @author Java Conversion from COBOL TRNVAL00
 * @version 1.0
 * @since 2024
 */
public class TRNVAL00 {
    
    private static final String PROGRAM_ID = "TRNVAL00";
    private static final String VERSION = "1.0 (Java 17)";
    
    private final Path inputFilePath;
    private final Path outputFilePath;
    private final Path errorReportPath;
    
    private TransactionFileProcessor fileProcessor;
    private TransactionValidator validator;
    private ErrorReporter errorReporter;
    
    private int totalRecords;
    private int validRecords;
    private int errorRecords;
    private int warningRecords;
    
    public TRNVAL00(String inputFile, String outputFile, String errorReport) {
        this.inputFilePath = Paths.get(inputFile);
        this.outputFilePath = Paths.get(outputFile);
        this.errorReportPath = Paths.get(errorReport);
        
        this.totalRecords = 0;
        this.validRecords = 0;
        this.errorRecords = 0;
        this.warningRecords = 0;
    }
    
    /**
     * Main entry point - corresponds to COBOL 0000-MAIN paragraph
     */
    public int execute() {
        Instant startTime = Instant.now();
        int returnCode = 0;
        
        try {
            System.out.println("================================================================================");
            System.out.println("                    TRANSACTION VALIDATION PROGRAM");
            System.out.println("                           " + PROGRAM_ID + " - " + VERSION);
            System.out.println("================================================================================");
            System.out.println("Start Time: " + startTime);
            System.out.println("Input File: " + inputFilePath);
            System.out.println("Output File: " + outputFilePath);
            System.out.println("Error Report: " + errorReportPath);
            System.out.println("================================================================================\n");
            
            initialize();
            process();
            cleanup();
            
            returnCode = errorReporter.determineReturnCode();
            
        } catch (IOException e) {
            System.err.println("CRITICAL ERROR: " + e.getMessage());
            e.printStackTrace();
            returnCode = 12;
        } catch (Exception e) {
            System.err.println("UNEXPECTED ERROR: " + e.getMessage());
            e.printStackTrace();
            returnCode = 16;
        } finally {
            Instant endTime = Instant.now();
            Duration duration = Duration.between(startTime, endTime);
            
            System.out.println("\n================================================================================");
            System.out.println("                           PROCESSING SUMMARY");
            System.out.println("================================================================================");
            System.out.println(String.format("Total Records Processed:  %,10d", totalRecords));
            System.out.println(String.format("Valid Records:            %,10d", validRecords));
            System.out.println(String.format("Records with Errors:      %,10d", errorRecords));
            System.out.println(String.format("Records with Warnings:    %,10d", warningRecords));
            System.out.println("--------------------------------------------------------------------------------");
            System.out.println(String.format("Processing Time:          %d seconds", duration.getSeconds()));
            System.out.println(String.format("Return Code:              %d", returnCode));
            System.out.println("================================================================================");
        }
        
        return returnCode;
    }
    
    /**
     * Initialize - corresponds to COBOL 1000-INITIALIZE paragraph
     */
    private void initialize() throws IOException {
        System.out.println("Initializing...");
        
        fileProcessor = new TransactionFileProcessor(inputFilePath, outputFilePath);
        validator = new TransactionValidator();
        errorReporter = new ErrorReporter(errorReportPath);
        
        fileProcessor.open();
        errorReporter.initialize();
        
        System.out.println("Initialization complete.\n");
    }
    
    /**
     * Process - corresponds to COBOL 2000-PROCESS paragraph
     */
    private void process() throws IOException {
        System.out.println("Processing transactions...\n");
        
        List<TransactionRecord> transactions = fileProcessor.readAllTransactions();
        totalRecords = transactions.size();
        
        System.out.println("Read " + totalRecords + " transaction records.");
        System.out.println("Validating transactions...\n");
        
        int checkpointInterval = 1000;
        int processedCount = 0;
        
        for (TransactionRecord transaction : transactions) {
            processedCount++;
            
            List<ValidationError> errors = validator.validate(transaction);
            
            if (errors.isEmpty()) {
                validRecords++;
                fileProcessor.writeValidTransaction(transaction);
            } else {
                boolean hasError = false;
                boolean hasWarning = false;
                
                for (ValidationError error : errors) {
                    errorReporter.addError(error);
                    
                    if (error.isError()) {
                        hasError = true;
                    } else if (error.isWarning()) {
                        hasWarning = true;
                    }
                }
                
                if (hasError) {
                    errorRecords++;
                } else if (hasWarning) {
                    warningRecords++;
                    validRecords++;
                    fileProcessor.writeValidTransaction(transaction);
                }
            }
            
            if (processedCount % checkpointInterval == 0) {
                System.out.println(String.format("Checkpoint: Processed %,d records...", processedCount));
            }
        }
        
        System.out.println("\nProcessing complete.");
    }
    
    /**
     * Cleanup - corresponds to COBOL 3000-CLEANUP paragraph
     */
    private void cleanup() throws IOException {
        System.out.println("\nCleaning up...");
        
        if (errorReporter != null) {
            errorReporter.writeFooter(totalRecords, validRecords);
            errorReporter.close();
        }
        
        if (fileProcessor != null) {
            fileProcessor.close();
        }
        
        System.out.println("Cleanup complete.");
    }
    
    /**
     * Main method - program entry point
     */
    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: java TRNVAL00 <input-file> <output-file> <error-report>");
            System.err.println();
            System.err.println("Example:");
            System.err.println("  java TRNVAL00 transactions.dat valid_transactions.dat error_report.txt");
            System.exit(16);
        }
        
        String inputFile = args[0];
        String outputFile = args[1];
        String errorReport = args[2];
        
        TRNVAL00 program = new TRNVAL00(inputFile, outputFile, errorReport);
        int returnCode = program.execute();
        
        System.exit(returnCode);
    }
}
