package com.investment.portfolio.common;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * File Handler - Java equivalent of standard COBOL file I/O operations.
 *
 * Abstracts sequential and indexed (VSAM KSDS) file access patterns
 * into modern Java file I/O, preserving the COBOL file status semantics.
 */
public class FileHandler implements Closeable {

    private static final Logger LOGGER = Logger.getLogger(FileHandler.class.getName());

    /** File status codes matching COBOL/VSAM conventions */
    public static final String STATUS_SUCCESS = "00";
    public static final String STATUS_EOF = "10";
    public static final String STATUS_DUPLICATE_KEY = "22";
    public static final String STATUS_NOT_FOUND = "23";
    public static final String STATUS_OPEN_ERROR = "35";
    public static final String STATUS_WRITE_ERROR = "48";

    private final Path filePath;
    private final String fileName;
    private BufferedReader reader;
    private BufferedWriter writer;
    private String lastStatus;

    public FileHandler(Path filePath) {
        this.filePath = filePath;
        this.fileName = filePath.getFileName().toString();
        this.lastStatus = STATUS_SUCCESS;
    }

    /**
     * Opens the file for reading.
     * Maps to OPEN INPUT in COBOL.
     */
    public String openInput() {
        try {
            if (!Files.exists(filePath)) {
                lastStatus = STATUS_OPEN_ERROR;
                return lastStatus;
            }
            reader = Files.newBufferedReader(filePath);
            lastStatus = STATUS_SUCCESS;
        } catch (IOException e) {
            LOGGER.warning("Error opening file for input: " + fileName + " - " + e.getMessage());
            lastStatus = STATUS_OPEN_ERROR;
        }
        return lastStatus;
    }

    /**
     * Opens the file for writing.
     * Maps to OPEN OUTPUT in COBOL.
     */
    public String openOutput() {
        try {
            writer = Files.newBufferedWriter(filePath);
            lastStatus = STATUS_SUCCESS;
        } catch (IOException e) {
            LOGGER.warning("Error opening file for output: " + fileName + " - " + e.getMessage());
            lastStatus = STATUS_OPEN_ERROR;
        }
        return lastStatus;
    }

    /**
     * Opens the file for both reading and writing (append mode).
     * Maps to OPEN I-O in COBOL.
     */
    public String openInputOutput() {
        try {
            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
            }
            reader = Files.newBufferedReader(filePath);
            writer = Files.newBufferedWriter(filePath,
                    java.nio.file.StandardOpenOption.APPEND);
            lastStatus = STATUS_SUCCESS;
        } catch (IOException e) {
            LOGGER.warning("Error opening file for I-O: " + fileName + " - " + e.getMessage());
            lastStatus = STATUS_OPEN_ERROR;
        }
        return lastStatus;
    }

    /**
     * Reads the next line from the file.
     * Maps to READ ... AT END ... in COBOL.
     *
     * @return the line read, or null if end of file
     */
    public String readLine() {
        try {
            if (reader == null) {
                lastStatus = STATUS_OPEN_ERROR;
                return null;
            }
            String line = reader.readLine();
            if (line == null) {
                lastStatus = STATUS_EOF;
                return null;
            }
            lastStatus = STATUS_SUCCESS;
            return line;
        } catch (IOException e) {
            LOGGER.warning("Error reading file: " + fileName + " - " + e.getMessage());
            lastStatus = STATUS_NOT_FOUND;
            return null;
        }
    }

    /**
     * Writes a line to the file.
     * Maps to WRITE record in COBOL.
     */
    public String writeLine(String line) {
        try {
            if (writer == null) {
                lastStatus = STATUS_WRITE_ERROR;
                return lastStatus;
            }
            writer.write(line);
            writer.newLine();
            lastStatus = STATUS_SUCCESS;
        } catch (IOException e) {
            LOGGER.warning("Error writing file: " + fileName + " - " + e.getMessage());
            lastStatus = STATUS_WRITE_ERROR;
        }
        return lastStatus;
    }

    /**
     * Returns the last file status code.
     * Maps to FILE STATUS IS WS-xxx-STATUS in COBOL.
     */
    public String getLastStatus() {
        return lastStatus;
    }

    public boolean isEndOfFile() {
        return STATUS_EOF.equals(lastStatus);
    }

    public boolean isSuccess() {
        return STATUS_SUCCESS.equals(lastStatus);
    }

    public String getFileName() {
        return fileName;
    }

    /**
     * Closes the file.
     * Maps to CLOSE in COBOL.
     */
    @Override
    public void close() throws IOException {
        if (reader != null) {
            reader.close();
        }
        if (writer != null) {
            writer.flush();
            writer.close();
        }
    }
}
