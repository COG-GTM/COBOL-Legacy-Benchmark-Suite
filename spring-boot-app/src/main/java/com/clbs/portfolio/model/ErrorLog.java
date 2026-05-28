package com.clbs.portfolio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Error log table.
 * From COBOL copybook: src/copybook/db2/DBTBLS.cpy (ERRLOG-RECORD)
 * and SQL: src/database/db2/ERRLOG.sql.
 */
@Entity
@Table(name = "error_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** EL-ERROR-TIMESTAMP — TIMESTAMP */
    @Column(name = "error_timestamp", nullable = false)
    private LocalDateTime errorTimestamp;

    /** EL-PROGRAM-ID — CHAR(8) */
    @Column(name = "program_id", length = 8, nullable = false)
    private String programId;

    /** EL-ERROR-TYPE — CHAR(1): S=System, A=Application, D=Data */
    @Enumerated(EnumType.STRING)
    @Column(name = "error_type", length = 15, nullable = false)
    private ErrorType errorType;

    /** EL-ERROR-SEVERITY — INTEGER: 1=Info, 2=Warning, 3=Error, 4=Severe */
    @Enumerated(EnumType.STRING)
    @Column(name = "error_severity", length = 10, nullable = false)
    private ErrorSeverity errorSeverity;

    /** EL-ERROR-CODE — CHAR(8) */
    @Column(name = "error_code", length = 8, nullable = false)
    private String errorCode;

    /** EL-ERROR-MESSAGE — VARCHAR(200) */
    @Column(name = "error_message", length = 200, nullable = false)
    private String errorMessage;

    /** EL-PROCESS-DATE — DATE */
    @Column(name = "process_date", nullable = false)
    private LocalDate processDate;

    /** EL-PROCESS-TIME — TIME */
    @Column(name = "process_time", nullable = false)
    private LocalTime processTime;

    /** EL-USER-ID — CHAR(8) */
    @Column(name = "user_id", length = 8, nullable = false)
    private String userId;

    /** EL-ADDITIONAL-INFO — VARCHAR(500) */
    @Column(name = "additional_info", length = 500)
    private String additionalInfo;

    public enum ErrorType {
        SYSTEM,
        APPLICATION,
        DATA
    }

    public enum ErrorSeverity {
        INFO,
        WARNING,
        ERROR,
        SEVERE
    }
}
