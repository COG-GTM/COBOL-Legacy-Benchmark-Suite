package com.clbs.portfolio.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_record")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "system_id", length = 8)
    private String systemId;

    @Column(name = "user_id", length = 8)
    private String userId;

    @Column(name = "program", length = 8)
    private String program;

    @Column(name = "terminal", length = 8)
    private String terminal;

    @Column(name = "audit_type", length = 4, nullable = false)
    private String auditType;

    @Column(name = "action", length = 8, nullable = false)
    private String action;

    @Column(name = "status", length = 4, nullable = false)
    private String status;

    @Column(name = "portfolio_id", length = 8)
    private String portfolioId;

    @Column(name = "account_no", length = 10)
    private String accountNo;

    @Column(name = "before_image", length = 500)
    private String beforeImage;

    @Column(name = "after_image", length = 500)
    private String afterImage;

    @Column(name = "message", length = 500)
    private String message;
}
