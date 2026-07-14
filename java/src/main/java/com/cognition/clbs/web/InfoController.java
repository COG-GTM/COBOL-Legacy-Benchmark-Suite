package com.cognition.clbs.web;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal root endpoint confirming the web layer is wired. Real inquiry and
 * portfolio REST endpoints (migrated from the CICS online layer) are added in
 * later phases under this package.
 */
@RestController
public class InfoController {

    @GetMapping("/")
    public Map<String, String> info() {
        return Map.of(
                "application", "clbs-portfolio",
                "status", "ok");
    }
}
