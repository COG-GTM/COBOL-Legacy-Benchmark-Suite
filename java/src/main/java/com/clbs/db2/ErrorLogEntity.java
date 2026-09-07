package com.clbs.db2;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** DBTBLS.cpy ERRLOG-RECORD, written by ERRHNDL and ERRPROC. */
@Entity
@Table(name = "errlog")
public class ErrorLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "log_timestamp", length = 26)
    private String timestamp;

    @Column(name = "program_id", length = 8)
    private String programId;

    @Column(name = "paragraph_name", length = 30)
    private String paragraphName;

    @Column(name = "sql_code")
    private Integer sqlCode;

    @Column(name = "cics_resp")
    private Integer cicsResp;

    @Column(name = "severity", length = 1)
    private String severity;

    @Column(name = "message_text", length = 80)
    private String message;

    @Column(name = "trace_id", length = 16)
    private String traceId;

    public Long getId() {
        return id;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getProgramId() {
        return programId;
    }

    public void setProgramId(String programId) {
        this.programId = programId;
    }

    public String getParagraphName() {
        return paragraphName;
    }

    public void setParagraphName(String paragraphName) {
        this.paragraphName = paragraphName;
    }

    public Integer getSqlCode() {
        return sqlCode;
    }

    public void setSqlCode(Integer sqlCode) {
        this.sqlCode = sqlCode;
    }

    public Integer getCicsResp() {
        return cicsResp;
    }

    public void setCicsResp(Integer cicsResp) {
        this.cicsResp = cicsResp;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
