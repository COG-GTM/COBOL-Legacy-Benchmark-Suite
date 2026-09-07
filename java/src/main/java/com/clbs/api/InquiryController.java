package com.clbs.api;

import com.clbs.online.InquiryCommArea;
import com.clbs.online.OnlineInquiryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP entry point for INQONLN. The CICS signed-on user (EXEC CICS ASSIGN USERID) is supplied by
 * the {@code X-User-Id} header.
 */
@RestController
@RequestMapping("/api/inquiry")
public class InquiryController {

    private final OnlineInquiryService service;

    public InquiryController(OnlineInquiryService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<OnlineInquiryService.Response> inquire(
            @RequestHeader(value = "X-User-Id", defaultValue = "") String userId,
            @RequestBody InquiryCommArea commArea) {
        OnlineInquiryService.Response response = service.process(commArea, userId);
        if (response.commArea().getResponseCode() != 0) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        return ResponseEntity.ok(response);
    }
}
