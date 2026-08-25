package com.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * JPA entity for the DB2 RTNCODES table ({@code src/database/db2/RTNCODES.sql}).
 * Primary key: (TIMESTAMP, PROGRAM_ID).
 */
@Entity
@Table(name = "RTNCODES")
public class ReturnCodeLog {

    @EmbeddedId
    private Key key;

    /** RETURN_CODE INTEGER. */
    @Column(name = "RETURN_CODE", nullable = false)
    private int returnCode;

    /** HIGHEST_CODE INTEGER. */
    @Column(name = "HIGHEST_CODE", nullable = false)
    private int highestCode;

    /** STATUS_CODE CHAR(1). */
    @Column(name = "STATUS_CODE", length = 1, nullable = false)
    private String statusCode;

    /** MESSAGE_TEXT VARCHAR(80). */
    @Column(name = "MESSAGE_TEXT", length = 80)
    private String messageText;

    /** Composite primary key (TIMESTAMP, PROGRAM_ID). */
    @Embeddable
    public static class Key implements Serializable {

        /** TIMESTAMP column (named LOG_TIMESTAMP to avoid the SQL reserved word). */
        @Column(name = "LOG_TIMESTAMP", nullable = false)
        private LocalDateTime timestamp;

        /** PROGRAM_ID CHAR(8). */
        @Column(name = "PROGRAM_ID", length = 8, nullable = false)
        private String programId;

        public Key() {}

        public Key(LocalDateTime timestamp, String programId) {
            this.timestamp = timestamp;
            this.programId = programId;
        }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        public String getProgramId() { return programId; }
        public void setProgramId(String programId) { this.programId = programId; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key key)) return false;
            return Objects.equals(timestamp, key.timestamp)
                    && Objects.equals(programId, key.programId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(timestamp, programId);
        }
    }

    public Key getKey() { return key; }
    public void setKey(Key key) { this.key = key; }
    public int getReturnCode() { return returnCode; }
    public void setReturnCode(int returnCode) { this.returnCode = returnCode; }
    public int getHighestCode() { return highestCode; }
    public void setHighestCode(int highestCode) { this.highestCode = highestCode; }
    public String getStatusCode() { return statusCode; }
    public void setStatusCode(String statusCode) { this.statusCode = statusCode; }
    public String getMessageText() { return messageText; }
    public void setMessageText(String messageText) { this.messageText = messageText; }
}
