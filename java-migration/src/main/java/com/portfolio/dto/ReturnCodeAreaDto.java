package com.portfolio.dto;
import java.time.LocalDateTime;
public record ReturnCodeAreaDto(String requestType,String programId,int currentCode,int highestCode,int newCode,String status,String message,int responseCode,LocalDateTime startTime,LocalDateTime endTime,int totalCodes,int maxCode,int minCode,int returnValue,int highestReturn,String returnStatus) {}
