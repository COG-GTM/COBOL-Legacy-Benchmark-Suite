package com.cognition.clbs.common.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDateTime;

/** Composite primary key of {@link ErrorLog}. */
@Embeddable
public record ErrorLogKey(
    @Column(name = "ERROR_TIMESTAMP", nullable = false) LocalDateTime errorTimestamp,
    @Column(name = "PROGRAM_ID", nullable = false, length = 8) String programId) {}
