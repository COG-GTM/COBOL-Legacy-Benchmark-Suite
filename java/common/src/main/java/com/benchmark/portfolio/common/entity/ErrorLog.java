package com.benchmark.portfolio.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Error log record, migrated from ERRHAND.cpy (ERR-MESSAGE structure).
 * Insert-only log table with surrogate identity key ERROR_LOG_ID (no VSAM key).
 * The ERR-CATEGORIES / ERR-RETURN-CODES / ERR-VSAM-STATUSES / ERR-VSAM-MSGS groups
 * are program constants, not persisted fields.
 */
@Entity
@Table(name = "ERROR_LOG")
public class ErrorLog {

    /** Surrogate identity key (log table; no copybook origin). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ERROR_LOG_ID")
    private Long errorLogId;

    /** ERR-DATE PIC X(10) (YYYY-MM-DD character). */
    @Column(name = "ERROR_DATE", nullable = false)
    private LocalDate errorDate;

    /** ERR-TIME PIC X(8) (HH.MM.SS / HH:MM:SS character). */
    @Column(name = "ERROR_TIME", nullable = false)
    private LocalTime errorTime;

    /** ERR-PROGRAM PIC X(8). */
    @Column(name = "PROGRAM_ID", length = 8, nullable = false)
    private String programId;

    /** ERR-CATEGORY PIC X(2); 'VS' VSAM, 'VL' Validation, 'PR' Processing, 'SY' System. */
    @Column(name = "ERROR_CATEGORY", columnDefinition = "CHAR(2)", length = 2, nullable = false)
    private String errorCategory;

    /** ERR-CODE PIC X(4). */
    @Column(name = "ERROR_CODE", columnDefinition = "CHAR(4)", length = 4, nullable = false)
    private String errorCode;

    /** ERR-SEVERITY PIC S9(4) COMP; values 0/4/8/12/16 per ERR-RETURN-CODES. */
    @Column(name = "ERROR_SEVERITY", nullable = false)
    private Short errorSeverity;

    /** ERR-TEXT PIC X(80). */
    @Column(name = "ERROR_TEXT", length = 80)
    private String errorText;

    /** ERR-DETAILS PIC X(256). */
    @Column(name = "ERROR_DETAILS", length = 256)
    private String errorDetails;

    public Long getErrorLogId() {
        return errorLogId;
    }

    public void setErrorLogId(Long errorLogId) {
        this.errorLogId = errorLogId;
    }

    public LocalDate getErrorDate() {
        return errorDate;
    }

    public void setErrorDate(LocalDate errorDate) {
        this.errorDate = errorDate;
    }

    public LocalTime getErrorTime() {
        return errorTime;
    }

    public void setErrorTime(LocalTime errorTime) {
        this.errorTime = errorTime;
    }

    public String getProgramId() {
        return programId;
    }

    public void setProgramId(String programId) {
        this.programId = programId;
    }

    public String getErrorCategory() {
        return errorCategory;
    }

    public void setErrorCategory(String errorCategory) {
        this.errorCategory = errorCategory;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public Short getErrorSeverity() {
        return errorSeverity;
    }

    public void setErrorSeverity(Short errorSeverity) {
        this.errorSeverity = errorSeverity;
    }

    public String getErrorText() {
        return errorText;
    }

    public void setErrorText(String errorText) {
        this.errorText = errorText;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }
}
