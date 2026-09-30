package com.portfolio.dto;

public record InquiryResponse(int responseCode, String errorMsg, Object payload) {}
