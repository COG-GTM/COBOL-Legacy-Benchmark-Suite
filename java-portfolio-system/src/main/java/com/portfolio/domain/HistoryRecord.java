package com.portfolio.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * History Record entity - migrated from COBOL copybook HISTREC.cpy
 * Represents audit history of changes
 */
@Entity
@Table(name = "history_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "portfolio_id", length = 8, nullable = false)
    private String portfolioId;

    @Column(name = "history_date", nullable = false)
    private LocalDate historyDate;

    @Column(name = "history_time")
    private LocalTime historyTime;

    @Column(name = "sequence_no", length = 4)
    private String sequenceNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "record_type", length = 2)
    private RecordType recordType;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_code", length = 1)
    private ActionCode actionCode;

    @Column(name = "before_image", length = 400)
    private String beforeImage;

    @Column(name = "after_image", length = 400)
    private String afterImage;

    @Column(name = "reason_code", length = 4)
    private String reasonCode;

    @Column(name = "process_date")
    private LocalDateTime processDate;

    @Column(name = "process_user", length = 8)
    private String processUser;

    @PrePersist
    protected void onCreate() {
        if (historyDate == null) {
            historyDate = LocalDate.now();
        }
        if (historyTime == null) {
            historyTime = LocalTime.now();
        }
        if (processDate == null) {
            processDate = LocalDateTime.now();
        }
    }

    public enum RecordType {
        PT, // Portfolio
        PS, // Position
        TR  // Transaction
    }

    public enum ActionCode {
        A, // Add
        C, // Change
        D  // Delete
    }
}
