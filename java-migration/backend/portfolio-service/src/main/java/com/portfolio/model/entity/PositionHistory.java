package com.portfolio.model.entity;

import com.portfolio.model.enums.ActionCode;
import com.portfolio.model.enums.HistoryRecordType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "position_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 8)
    @Column(name = "portfolio_id", nullable = false, length = 8)
    private String portfolioId;

    @Column(name = "history_date")
    private LocalDate historyDate;

    @Column(name = "history_time")
    private LocalTime historyTime;

    @Size(max = 4)
    @Column(name = "sequence_no", length = 4)
    private String sequenceNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "record_type")
    private HistoryRecordType recordType;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_code")
    private ActionCode actionCode;

    @Column(name = "before_image", columnDefinition = "TEXT")
    private String beforeImage;

    @Column(name = "after_image", columnDefinition = "TEXT")
    private String afterImage;

    @Size(max = 4)
    @Column(name = "reason_code", length = 4)
    private String reasonCode;

    @Column(name = "process_date")
    private LocalDateTime processDate;

    @Size(max = 8)
    @Column(name = "process_user", length = 8)
    private String processUser;

    @Size(max = 10)
    @Column(name = "account_no", length = 10)
    private String accountNo;

    @Size(max = 10)
    @Column(name = "security_id", length = 10)
    private String securityId;

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
}
