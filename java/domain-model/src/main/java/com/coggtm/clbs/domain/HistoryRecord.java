package com.coggtm.clbs.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "position_history", indexes = {
    @Index(name = "idx_hist_portfolio_id", columnList = "portfolio_id"),
    @Index(name = "idx_hist_date", columnList = "history_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(max = 10)
    @Column(name = "portfolio_id", nullable = false, length = 10)
    private String portfolioId;

    @NotNull
    @Column(name = "history_date", nullable = false)
    private LocalDate historyDate;

    @NotNull
    @Column(name = "history_time", nullable = false)
    private LocalTime historyTime;

    @NotNull
    @Size(max = 4)
    @Column(name = "sequence_number", nullable = false, length = 4)
    private String sequenceNumber;

    @NotNull
    @Size(max = 2)
    @Column(name = "record_type", nullable = false, length = 2)
    private String recordType;

    @NotNull
    @Size(max = 1)
    @Column(name = "action_code", nullable = false, length = 1)
    private String actionCode;

    @Column(name = "before_image", length = 400)
    private String beforeImage;

    @Column(name = "after_image", length = 400)
    private String afterImage;

    @Size(max = 4)
    @Column(name = "reason_code", length = 4)
    private String reasonCode;

    @Column(name = "process_date")
    private LocalDateTime processDate;

    @Size(max = 8)
    @Column(name = "process_user", length = 8)
    private String processUser;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
