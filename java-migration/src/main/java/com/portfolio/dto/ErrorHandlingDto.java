package com.portfolio.dto;

import java.time.LocalDateTime;

public record ErrorHandlingDto(
    String program,
    String paragraph,
    Severity severity,
    Action action,
    String message,
    String traceId,
    LocalDateTime timestamp) {
  public enum Severity {
    FATAL,
    WARNING,
    INFO
  }

  public enum Action {
    RETURN,
    CONTINUE,
    ABEND
  }
}
