package com.portfolio.modernization.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1/health")
@Tag(name = "Health", description = "Health check APIs")
public class HealthController {

    @GetMapping
    @Operation(summary = "Health check endpoint")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("application", "Portfolio Modernization Service");
        health.put("version", "1.0.0-SNAPSHOT");
        return ResponseEntity.ok(health);
    }
}
