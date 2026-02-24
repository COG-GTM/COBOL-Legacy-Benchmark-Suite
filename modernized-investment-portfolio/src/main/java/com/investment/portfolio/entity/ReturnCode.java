package com.investment.portfolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * JPA Entity mapping for the Return Code Logging table (RTNCODES).
 *
 * COBOL Source: RTNCODE.cpy
 * DB2 Source: RTNCODES.sql
 *   PK: (TIMESTAMP, PROGRAM_ID)
 *
 * Status codes: refer to COMMON.cpy STATUS-CODES
 */
@Entity
@Table(name = "return_codes")
public class ReturnCode {

    @EmbeddedId
    private ReturnCodeId id;

    /**
     * Return Code value.
     * DB2: RETURN_CODE INTEGER NOT NULL
     */
    @Column(name = "return_code", nullable = false)
    @NotNull
    private Integer returnCode;

    /**
     * Highest Return Code encountered.
     * DB2: HIGHEST_CODE INTEGER NOT NULL
     */
    @Column(name = "highest_code", nullable = false)
    @NotNull
    private Integer highestCode;

    /**
     * Status Code.
     * DB2: STATUS_CODE CHAR(1) NOT NULL
     */
    @Column(name = "status_code", length = 1, nullable = false)
    @NotNull
    @Size(max = 1)
    private String statusCode;

    /**
     * Message Text (optional).
     * DB2: MESSAGE_TEXT VARCHAR(80)
     */
    @Column(name = "message_text", length = 80)
    @Size(max = 80)
    private String messageText;

    public ReturnCode() {
    }

    // --- Getters and Setters ---

    public ReturnCodeId getId() {
        return id;
    }

    public void setId(ReturnCodeId id) {
        this.id = id;
    }

    public Integer getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(Integer returnCode) {
        this.returnCode = returnCode;
    }

    public Integer getHighestCode() {
        return highestCode;
    }

    public void setHighestCode(Integer highestCode) {
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
