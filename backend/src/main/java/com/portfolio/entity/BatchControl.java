package com.portfolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Batch Control entity - migrated from VSAM BCHCTL file.
 * Source: BCHCTL copybook, data-dictionary.md section 2.4
 *
 * Status: 'W'=Waiting, 'P'=In-Process, 'C'=Complete, 'E'=Error
 */
@Entity
@Table(name = "batch_control")
public class BatchControl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "process_date", nullable = false)
    private LocalDate processDate;

    @Column(name = "process_id", length = 8, nullable = false)
    private String processId;

    @Column(name = "status", length = 1, nullable = false)
    private String status = "W";

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "record_count")
    private Long recordCount = 0L;

    @Column(name = "error_count")
    private Long errorCount = 0L;

    @Column(name = "last_position")
    private Long lastPosition = 0L;

    @Column(name = "return_code")
    private Integer returnCode = 0;

    @Column(name = "message", length = 200)
    private String message;

    public BatchControl() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getProcessDate() { return processDate; }
    public void setProcessDate(LocalDate processDate) { this.processDate = processDate; }
    public String getProcessId() { return processId; }
    public void setProcessId(String processId) { this.processId = processId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public Long getRecordCount() { return recordCount; }
    public void setRecordCount(Long recordCount) { this.recordCount = recordCount; }
    public Long getErrorCount() { return errorCount; }
    public void setErrorCount(Long errorCount) { this.errorCount = errorCount; }
    public Long getLastPosition() { return lastPosition; }
    public void setLastPosition(Long lastPosition) { this.lastPosition = lastPosition; }
    public Integer getReturnCode() { return returnCode; }
    public void setReturnCode(Integer returnCode) { this.returnCode = returnCode; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isWaiting() { return "W".equals(status); }
    public boolean isInProcess() { return "P".equals(status); }
    public boolean isComplete() { return "C".equals(status); }
    public boolean isError() { return "E".equals(status); }
}
