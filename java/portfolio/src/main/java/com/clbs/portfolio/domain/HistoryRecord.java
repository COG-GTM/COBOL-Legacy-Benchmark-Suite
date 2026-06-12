package com.clbs.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * History Record — JPA mapping of HISTREC.cpy (HISTORY-RECORD).
 * Stores before/after images for portfolio, position, and transaction changes.
 */
@Entity
@Table(name = "history_record")
@Getter
@Setter
@NoArgsConstructor
public class HistoryRecord {

    @EmbeddedId
    private HistoryKey key;

    /** HIST-RECORD-TYPE PIC X(02): PT=Portfolio, PS=Position, TR=Transaction. */
    @Column(name = "hist_record_type", length = 2, nullable = false)
    private String recordType;

    /** HIST-ACTION-CODE PIC X(01): A=Add, C=Change, D=Delete. */
    @Column(name = "hist_action_code", length = 1, nullable = false)
    private String actionCode;

    /** HIST-BEFORE-IMAGE PIC X(400). */
    @Column(name = "hist_before_image", length = 400)
    private String beforeImage;

    /** HIST-AFTER-IMAGE PIC X(400). */
    @Column(name = "hist_after_image", length = 400)
    private String afterImage;

    /** HIST-REASON-CODE PIC X(04). */
    @Column(name = "hist_reason_code", length = 4)
    private String reasonCode;

    /** HIST-PROCESS-DATE PIC X(26). */
    @Column(name = "hist_process_date", length = 26, nullable = false)
    private String processDate;

    /** HIST-PROCESS-USER PIC X(08). */
    @Column(name = "hist_process_user", length = 8, nullable = false)
    private String processUser;

    /** HIST-FILLER PIC X(50). */
    @Column(name = "hist_filler", length = 50)
    private String filler;
}
