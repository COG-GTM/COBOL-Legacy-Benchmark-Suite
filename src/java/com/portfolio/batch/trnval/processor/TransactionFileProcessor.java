package com.portfolio.batch.trnval.processor;

import com.portfolio.batch.trnval.model.TransactionRecord;
import com.portfolio.batch.trnval.model.TransactionRecord.TransactionStatus;
import com.portfolio.batch.trnval.model.TransactionRecord.TransactionType;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Transaction File Processor - Handles sequential file processing
 * 
 * Corresponds to COBOL sequential file processing patterns:
 * - SELECT TRANFILE ASSIGN TO TRANFILE
 *   ORGANIZATION IS SEQUENTIAL
 *   ACCESS MODE IS SEQUENTIAL
 *   FILE STATUS IS WS-TRAN-STATUS
 * 
 * Processes fixed-format transaction records similar to COBOL:
 * - Reads transaction file line by line
 * - Parses fixed-position fields
 * - Writes valid transactions to output file
 * - Handles file status and EOF conditions
 */
public class TransactionFileProcessor {
    
    private static final int FIELD_DATE_START = 0;
    private static final int FIELD_DATE_LENGTH = 8;
    private static final int FIELD_TIME_START = 8;
    private static final int FIELD_TIME_LENGTH = 6;
    private static final int FIELD_PORTFOLIO_START = 14;
    private static final int FIELD_PORTFOLIO_LENGTH = 8;
    private static final int FIELD_SEQUENCE_START = 22;
    private static final int FIELD_SEQUENCE_LENGTH = 6;
    private static final int FIELD_INVESTMENT_START = 28;
    private static final int FIELD_INVESTMENT_LENGTH = 10;
    private static final int FIELD_TYPE_START = 38;
    private static final int FIELD_TYPE_LENGTH = 2;
    private static final int FIELD_QUANTITY_START = 40;
    private static final int FIELD_QUANTITY_LENGTH = 15;
    private static final int FIELD_PRICE_START = 55;
    private static final int FIELD_PRICE_LENGTH = 15;
    private static final int FIELD_AMOUNT_START = 70;
    private static final int FIELD_AMOUNT_LENGTH = 15;
    private static final int FIELD_CURRENCY_START = 85;
    private static final int FIELD_CURRENCY_LENGTH = 3;
    private static final int FIELD_STATUS_START = 88;
    private static final int FIELD_STATUS_LENGTH = 1;
    
    private final Path inputFilePath;
    private final Path outputFilePath;
    private BufferedReader reader;
    private BufferedWriter writer;
    private int lineNumber;
    
    public TransactionFileProcessor(Path inputFilePath, Path outputFilePath) {
        this.inputFilePath = inputFilePath;
        this.outputFilePath = outputFilePath;
        this.lineNumber = 0;
    }
    
    public void open() throws IOException {
        reader = Files.newBufferedReader(inputFilePath);
        writer = Files.newBufferedWriter(outputFilePath, 
                StandardOpenOption.CREATE, 
                StandardOpenOption.TRUNCATE_EXISTING);
    }
    
    public List<TransactionRecord> readAllTransactions() throws IOException {
        List<TransactionRecord> transactions = new ArrayList<>();
        String line;
        
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            
            if (line.trim().isEmpty() || line.startsWith("#")) {
                continue;
            }
            
            try {
                TransactionRecord transaction = parseLine(line);
                transaction.setLineNumber(lineNumber);
                transactions.add(transaction);
            } catch (Exception e) {
                System.err.println("Error parsing line " + lineNumber + ": " + e.getMessage());
            }
        }
        
        return transactions;
    }
    
    private TransactionRecord parseLine(String line) {
        TransactionRecord transaction = new TransactionRecord();
        
        transaction.setDate(extractField(line, FIELD_DATE_START, FIELD_DATE_LENGTH));
        transaction.setTime(extractField(line, FIELD_TIME_START, FIELD_TIME_LENGTH));
        transaction.setPortfolioId(extractField(line, FIELD_PORTFOLIO_START, FIELD_PORTFOLIO_LENGTH));
        transaction.setSequenceNo(extractField(line, FIELD_SEQUENCE_START, FIELD_SEQUENCE_LENGTH));
        transaction.setInvestmentId(extractField(line, FIELD_INVESTMENT_START, FIELD_INVESTMENT_LENGTH));
        
        String typeCode = extractField(line, FIELD_TYPE_START, FIELD_TYPE_LENGTH);
        transaction.setType(TransactionType.fromCode(typeCode));
        
        String quantityStr = extractField(line, FIELD_QUANTITY_START, FIELD_QUANTITY_LENGTH);
        transaction.setQuantity(parseBigDecimal(quantityStr));
        
        String priceStr = extractField(line, FIELD_PRICE_START, FIELD_PRICE_LENGTH);
        transaction.setPrice(parseBigDecimal(priceStr));
        
        String amountStr = extractField(line, FIELD_AMOUNT_START, FIELD_AMOUNT_LENGTH);
        transaction.setAmount(parseBigDecimal(amountStr));
        
        transaction.setCurrency(extractField(line, FIELD_CURRENCY_START, FIELD_CURRENCY_LENGTH));
        
        String statusCode = extractField(line, FIELD_STATUS_START, FIELD_STATUS_LENGTH);
        transaction.setStatus(TransactionStatus.fromCode(statusCode));
        
        return transaction;
    }
    
    private String extractField(String line, int start, int length) {
        if (line.length() < start) {
            return "";
        }
        
        int end = Math.min(start + length, line.length());
        return line.substring(start, end).trim();
    }
    
    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
    
    public void writeValidTransaction(TransactionRecord transaction) throws IOException {
        if (writer != null) {
            String line = formatTransactionLine(transaction);
            writer.write(line);
            writer.newLine();
        }
    }
    
    private String formatTransactionLine(TransactionRecord transaction) {
        return String.format("%-8s%-6s%-8s%-6s%-10s%-2s%15s%15s%15s%-3s%-1s",
                transaction.getDate() != null ? transaction.getDate() : "",
                transaction.getTime() != null ? transaction.getTime() : "",
                transaction.getPortfolioId() != null ? transaction.getPortfolioId() : "",
                transaction.getSequenceNo() != null ? transaction.getSequenceNo() : "",
                transaction.getInvestmentId() != null ? transaction.getInvestmentId() : "",
                transaction.getType() != null ? transaction.getType().getCode() : "",
                transaction.getQuantity() != null ? transaction.getQuantity().toString() : "0",
                transaction.getPrice() != null ? transaction.getPrice().toString() : "0",
                transaction.getAmount() != null ? transaction.getAmount().toString() : "0",
                transaction.getCurrency() != null ? transaction.getCurrency() : "",
                transaction.getStatus() != null ? transaction.getStatus().getCode() : "");
    }
    
    public void close() throws IOException {
        if (reader != null) {
            reader.close();
        }
        if (writer != null) {
            writer.flush();
            writer.close();
        }
    }
    
    public int getLineNumber() {
        return lineNumber;
    }
}
