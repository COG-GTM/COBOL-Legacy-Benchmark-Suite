package com.portfolio.dto;

public record ReturnHandlingDto(
    int returnCode,
    int reasonCode,
    String moduleId,
    String functionId,
    String programName,
    String paragraphName,
    String errorCode,
    String errorText,
    String actionFlag,
    int retryCount,
    int maxRetries) {}
