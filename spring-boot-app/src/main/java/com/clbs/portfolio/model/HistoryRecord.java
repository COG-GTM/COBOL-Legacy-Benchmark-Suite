package com.clbs.portfolio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * History/change-tracking record.
 * From COBOL copybook: src/copybook/common/HISTREC.cpy (HISTORY-RECORD).
 */
@Entity
@Table(name = "history_record")
@IdClass(HistoryRecordId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoryRecord {

    /** HIST-PORTFOLIO-ID — PIC X(08) */
    @Id
    @Column(name = "portfolio_id", length = 8, nullable = false)
    private String portfolioId;

    /** HIST-DATE — PIC X(08), YYYYMMDD */
    @Id
    @Column(name = "hist_date", length = 8, nullable = false)
    private String histDate;

    /** HIST-TIME — PIC X(06), HHMMSS */
    @Id
    @Column(name = "hist_time", length = 6, nullable = false)
    private String histTime;

    /** HIST-SEQ-NO — PIC X(04) */
    @Id
    @Column(name = "seq_no", length = 4, nullable = false)
    private String seqNo;

    /** HIST-RECORD-TYPE — PIC X(02): PT=Portfolio, PS=Position, TR=Transaction */
    @Enumerated(EnumType.STRING)
    @Column(name = "record_type", length = 15, nullable = false)
    private RecordType recordType;

    /** HIST-ACTION-CODE — PIC X(01): A=Add, C=Change, D=Delete */
    @Enumerated(EnumType.STRING)
    @Column(name = "action_code", length = 10, nullable = false)
    private ActionCode actionCode;

    /** HIST-BEFORE-IMAGE — PIC X(400) */
    @Column(name = "before_image", length = 4000)
    private String beforeImage;

    /** HIST-AFTER-IMAGE — PIC X(400) */
    @Column(name = "after_image", length = 4000)
    private String afterImage;

    /** HIST-REASON-CODE — PIC X(04) */
    @Column(name = "reason_code", length = 4)
    private String reasonCode;

    /** HIST-PROCESS-DATE — PIC X(26) */
    @Column(name = "process_date")
    private LocalDateTime processDate;

    /** HIST-PROCESS-USER — PIC X(08) */
    @Column(name = "process_user", length = 8)
    private String processUser;

    public enum RecordType {
        PORTFOLIO,
        POSITION,
        TRANSACTION
    }

    public enum ActionCode {
        ADD,
        CHANGE,
        DELETE
    }
}
