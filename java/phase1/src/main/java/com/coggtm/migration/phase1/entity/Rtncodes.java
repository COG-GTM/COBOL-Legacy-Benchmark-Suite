package com.coggtm.migration.phase1.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "rtncodes")
@IdClass(Rtncodes.RtncodesId.class)
public class Rtncodes {

    @Id
    @Column(name = "log_timestamp", nullable = false)
    private LocalDateTime logTimestamp;

    @Id
    @Column(name = "program_id", length = 8, nullable = false)
    private String programId;

    @Column(name = "return_code", nullable = false)
    private Integer returnCode;

    @Column(name = "highest_code", nullable = false)
    private Integer highestCode;

    @Column(name = "status_code", length = 1, nullable = false)
    private String statusCode;

    @Column(name = "message_text", length = 80)
    private String messageText;

    public Rtncodes() {
    }

    public LocalDateTime getLogTimestamp() { return logTimestamp; }
    public void setLogTimestamp(LocalDateTime logTimestamp) { this.logTimestamp = logTimestamp; }

    public String getProgramId() { return programId; }
    public void setProgramId(String programId) { this.programId = programId; }

    public Integer getReturnCode() { return returnCode; }
    public void setReturnCode(Integer returnCode) { this.returnCode = returnCode; }

    public Integer getHighestCode() { return highestCode; }
    public void setHighestCode(Integer highestCode) { this.highestCode = highestCode; }

    public String getStatusCode() { return statusCode; }
    public void setStatusCode(String statusCode) { this.statusCode = statusCode; }

    public String getMessageText() { return messageText; }
    public void setMessageText(String messageText) { this.messageText = messageText; }

    public static class RtncodesId implements Serializable {
        private LocalDateTime logTimestamp;
        private String programId;

        public RtncodesId() {
        }

        public RtncodesId(LocalDateTime logTimestamp, String programId) {
            this.logTimestamp = logTimestamp;
            this.programId = programId;
        }

        public LocalDateTime getLogTimestamp() { return logTimestamp; }
        public void setLogTimestamp(LocalDateTime logTimestamp) { this.logTimestamp = logTimestamp; }

        public String getProgramId() { return programId; }
        public void setProgramId(String programId) { this.programId = programId; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RtncodesId)) return false;
            RtncodesId that = (RtncodesId) o;
            return Objects.equals(logTimestamp, that.logTimestamp)
                    && Objects.equals(programId, that.programId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(logTimestamp, programId);
        }
    }
}
