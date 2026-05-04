package com.coggtm.portfolio.domain;

import com.coggtm.portfolio.domain.enums.AuditAction;
import com.coggtm.portfolio.domain.enums.AuditType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * JPA entity mapped from COBOL copybook AUDITLOG.cpy.
 *
 * <p>COBOL field mapping:</p>
 * <ul>
 *   <li>AUD-TIMESTAMP (PIC X(26)) → timestamp (LocalDateTime)</li>
 *   <li>AUD-TYPE 88-levels (TRAN/USER/SYST) → AuditType enum</li>
 *   <li>AUD-ACTION 88-levels → AuditAction enum</li>
 *   <li>AUD-BEFORE-IMAGE / AUD-AFTER-IMAGE (PIC X(100)) → TEXT/CLOB columns</li>
 * </ul>
 */
@Entity
@Table(name = "AUDIT_LOG")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "AUDIT_TIMESTAMP", nullable = false)
    private LocalDateTime timestamp;

    @Size(max = 8)
    @Column(name = "SYSTEM_ID", length = 8)
    private String systemId;

    @Size(max = 8)
    @Column(name = "USER_ID", length = 8)
    private String userId;

    @Size(max = 8)
    @Column(name = "PROGRAM", length = 8)
    private String program;

    @Size(max = 8)
    @Column(name = "TERMINAL", length = 8)
    private String terminal;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "AUDIT_TYPE", length = 4, nullable = false)
    private AuditType auditType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "AUDIT_ACTION", length = 8, nullable = false)
    private AuditAction action;

    @Size(max = 4)
    @Column(name = "STATUS", length = 4)
    private String status;

    @Size(max = 8)
    @Column(name = "PORTFOLIO_ID", length = 8)
    private String portfolioId;

    @Size(max = 10)
    @Column(name = "ACCOUNT_NO", length = 10)
    private String accountNo;

    @Lob
    @Column(name = "BEFORE_IMAGE", columnDefinition = "TEXT")
    private String beforeImage;

    @Lob
    @Column(name = "AFTER_IMAGE", columnDefinition = "TEXT")
    private String afterImage;

    @Size(max = 100)
    @Column(name = "MESSAGE", length = 100)
    private String message;
}
