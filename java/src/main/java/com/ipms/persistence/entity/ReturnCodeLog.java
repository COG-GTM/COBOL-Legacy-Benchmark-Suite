package com.ipms.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/** RTNCODES table from {@code src/database/db2/RTNCODES.sql}. */
@Entity
@Table(name = "RTNCODES")
@IdClass(ReturnCodeLog.Key.class)
public class ReturnCodeLog {

    @Id
    @Column(name = "LOG_TIMESTAMP", nullable = false)
    private LocalDateTime timestamp;

    @Id
    @Column(name = "PROGRAM_ID", length = 8, nullable = false)
    private String programId;

    @Column(name = "RETURN_CODE", nullable = false)
    private int returnCode;

    @Column(name = "HIGHEST_CODE", nullable = false)
    private int highestCode;

    @Column(name = "STATUS_CODE", length = 1, nullable = false)
    private String statusCode;

    @Column(name = "MESSAGE_TEXT", length = 80)
    private String messageText;

    public static class Key implements Serializable {
        private LocalDateTime timestamp;
        private String programId;

        public Key() {
        }

        public Key(LocalDateTime timestamp, String programId) {
            this.timestamp = timestamp;
            this.programId = programId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Key key)) {
                return false;
            }
            return Objects.equals(timestamp, key.timestamp)
                    && Objects.equals(programId, key.programId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(timestamp, programId);
        }
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getProgramId() {
        return programId;
    }

    public void setProgramId(String programId) {
        this.programId = programId;
    }

    public int getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }

    public int getHighestCode() {
        return highestCode;
    }

    public void setHighestCode(int highestCode) {
        this.highestCode = highestCode;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }
}
