package com.portfolio.model.entity;

import com.portfolio.model.enums.AuditAction;
import com.portfolio.model.enums.AuditStatus;
import com.portfolio.model.enums.AuditType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Size(max = 8)
    @Column(name = "system_id", length = 8)
    private String systemId;

    @Size(max = 8)
    @Column(name = "user_id", length = 8)
    private String userId;

    @Size(max = 8)
    @Column(name = "program", length = 8)
    private String program;

    @Size(max = 8)
    @Column(name = "terminal", length = 8)
    private String terminal;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private AuditType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "action")
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private AuditStatus status;

    @Size(max = 8)
    @Column(name = "portfolio_id", length = 8)
    private String portfolioId;

    @Size(max = 10)
    @Column(name = "account_no", length = 10)
    private String accountNo;

    @Column(name = "before_image", columnDefinition = "TEXT")
    private String beforeImage;

    @Column(name = "after_image", columnDefinition = "TEXT")
    private String afterImage;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
