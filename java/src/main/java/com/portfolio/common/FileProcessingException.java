package com.portfolio.common;

/**
 * Replaces COBOL two-character FILE STATUS checks (e.g. HISTLD00's
 * {@code IF WS-TH-STATUS NOT = '00' ... PERFORM 9000-ERROR-ROUTINE}).
 *
 * <p>Convention: any non-'00' FILE STATUS branch in COBOL becomes a thrown
 * {@code FileProcessingException} in Java; the original two-character status
 * (when meaningful) is preserved in {@link #getFileStatus()}.
 */
public class FileProcessingException extends RuntimeException {

    /** Original COBOL FILE STATUS value (e.g. "23" = record not found), or null. */
    private final String fileStatus;

    public FileProcessingException(String message) {
        this(message, null, null);
    }

    public FileProcessingException(String message, String fileStatus) {
        this(message, fileStatus, null);
    }

    public FileProcessingException(String message, String fileStatus, Throwable cause) {
        super(message, cause);
        this.fileStatus = fileStatus;
    }

    public String getFileStatus() {
        return fileStatus;
    }
}
