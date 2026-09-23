package com.cognition.clbs.common.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDateTime;

/** Composite primary key of {@link ReturnCodeLog}. */
@Embeddable
public record ReturnCodeLogKey(
    @Column(name = "LOG_TIMESTAMP", nullable = false) LocalDateTime logTimestamp,
    @Column(name = "PROGRAM_ID", nullable = false, length = 8) String programId) {}
