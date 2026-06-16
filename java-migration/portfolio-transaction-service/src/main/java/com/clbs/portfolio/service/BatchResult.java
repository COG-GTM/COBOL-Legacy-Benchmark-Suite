package com.clbs.portfolio.service;

/**
 * Batch run counters, mirroring PORTTRAN's {@code WS-COUNTERS} and the totals
 * displayed by {@code 3000-TERMINATE}:
 * <pre>
 *   DISPLAY 'Transactions Read:    ' WS-READ-COUNT
 *   DISPLAY 'Transactions Process: ' WS-PROCESS-COUNT
 *   DISPLAY 'Errors Encountered:   ' WS-ERROR-COUNT
 * </pre>
 *
 * @param read       transactions read ({@code WS-READ-COUNT})
 * @param processed  transactions processed successfully ({@code WS-PROCESS-COUNT})
 * @param errors     errors encountered ({@code WS-ERROR-COUNT})
 * @param halted     {@code true} if the run stopped early on the &gt;100 error circuit breaker
 */
public record BatchResult(long read, long processed, long errors, boolean halted) {
}
