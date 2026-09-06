package com.cog.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/** RTNCODES.sql: per-program return code log analysed by RTNANA00. */
@Entity
@Table(name = "RTNCODES")
public class ReturnCodeLog {

    @Embeddable
    public static class Key implements Serializable {

        @Column(name = "LOG_TIMESTAMP", nullable = false)
        private LocalDateTime timestamp;

        @Column(name = "PROGRAM_ID", length = 8, nullable = false)
        private String programId;

        protected Key() {
        }

        public Key(LocalDateTime timestamp, String programId) {
            this.timestamp = timestamp;
            this.programId = programId;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public String getProgramId() {
            return programId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key key)) return false;
            return Objects.equals(timestamp, key.timestamp) && Objects.equals(programId, key.programId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(timestamp, programId);
        }
    }

    @EmbeddedId
    private Key id;

    @Column(name = "RETURN_CODE", nullable = false)
    private int returnCode;

    @Column(name = "HIGHEST_CODE", nullable = false)
    private int highestCode;

    /** RTNCODE.cpy RC-STATUS: S=success, W=warning, E=error. */
    @Column(name = "STATUS_CODE", length = 1, nullable = false)
    private String statusCode;

    @Column(name = "MESSAGE_TEXT", length = 80)
    private String messageText;

    protected ReturnCodeLog() {
    }

    public ReturnCodeLog(Key id, int returnCode, int highestCode, String statusCode, String messageText) {
        this.id = id;
        this.returnCode = returnCode;
        this.highestCode = highestCode;
        this.statusCode = statusCode;
        this.messageText = messageText;
    }

    public Key getId() {
        return id;
    }

    public int getReturnCode() {
        return returnCode;
    }

    public int getHighestCode() {
        return highestCode;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public String getMessageText() {
        return messageText;
    }
}
