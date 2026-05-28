package com.clbs.portfolio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Return code management area (embeddable, not a standalone entity).
 * From COBOL copybook: src/copybook/common/RTNCODE.cpy (RETURN-CODE-AREA).
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnCodeArea {

    /** RC-REQUEST-TYPE — PIC X */
    @Column(name = "request_type", length = 1)
    private String requestType;

    /** RC-PROGRAM-ID — PIC X(8) */
    @Column(name = "rc_program_id", length = 8)
    private String programId;

    /** RC-CURRENT-CODE — PIC S9(4) COMP */
    @Column(name = "current_code")
    private Integer currentCode;

    /** RC-HIGHEST-CODE — PIC S9(4) COMP */
    @Column(name = "highest_code")
    private Integer highestCode;

    /** RC-NEW-CODE — PIC S9(4) COMP */
    @Column(name = "new_code")
    private Integer newCode;

    /** RC-STATUS — PIC X */
    @Column(name = "rc_status", length = 1)
    private String status;

    /** RC-MESSAGE — PIC X(80) */
    @Column(name = "rc_message", length = 80)
    private String message;

    /** RC-RESPONSE-CODE — PIC S9(8) COMP */
    @Column(name = "response_code")
    private Integer responseCode;

    /** RC-START-TIME — PIC X(26) (from RC-ANALYSIS-DATA) */
    @Column(name = "analysis_start_time")
    private LocalDateTime analysisStartTime;

    /** RC-END-TIME — PIC X(26) (from RC-ANALYSIS-DATA) */
    @Column(name = "analysis_end_time")
    private LocalDateTime analysisEndTime;

    /** RC-TOTAL-CODES — PIC S9(8) COMP */
    @Column(name = "total_codes")
    private Integer totalCodes;

    /** RC-MAX-CODE — PIC S9(4) COMP */
    @Column(name = "max_code")
    private Integer maxCode;

    /** RC-MIN-CODE — PIC S9(4) COMP */
    @Column(name = "min_code")
    private Integer minCode;
}
