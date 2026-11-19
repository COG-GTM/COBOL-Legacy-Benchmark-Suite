package com.clbs.portfolio.batch;

import java.util.List;
import java.util.Map;

/**
 * Result of a batch run: the console output the COBOL program would DISPLAY, the run counters, and any audit
 * records written to a sequential audit file (PORTDEL AUDFILE).
 */
public record BatchReport(String program, Map<String, Integer> counts, List<String> output, List<String> auditTrail) {
}
