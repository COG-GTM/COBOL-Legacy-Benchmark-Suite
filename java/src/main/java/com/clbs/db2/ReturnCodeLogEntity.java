package com.clbs.db2;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** RTNCODES table written by RTNCDE00 P400-LOG-RETURN-CODE and read by RTNANA00. */
@Entity
@Table(name = "rtncodes")
public class ReturnCodeLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "log_timestamp", length = 26)
    private String timestamp;

    @Column(name = "program_id", length = 8)
    private String programId;

    @Column(name = "return_code")
    private int returnCode;

    @Column(name = "highest_code")
    private int highestCode;

    @Column(name = "status_code", length = 1)
    private String statusCode;

    @Column(name = "message_text", length = 80)
    private String message;

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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
