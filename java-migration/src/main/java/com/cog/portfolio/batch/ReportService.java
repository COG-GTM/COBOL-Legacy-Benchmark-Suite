package com.cog.portfolio.batch;

import com.cog.portfolio.common.ScaleConverter;
import com.cog.portfolio.domain.AuditLog;
import com.cog.portfolio.domain.AuditStatus;
import com.cog.portfolio.domain.ErrorLog;
import com.cog.portfolio.domain.Position;
import com.cog.portfolio.domain.ReturnCodeLog;
import com.cog.portfolio.repository.AuditLogRepository;
import com.cog.portfolio.repository.ErrorLogRepository;
import com.cog.portfolio.repository.PositionHistoryRepository;
import com.cog.portfolio.repository.PositionRepository;
import com.cog.portfolio.repository.ReturnCodeLogRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Report programs RPTPOS00 / RPTAUD00 / RPTSTA00 / RTNANA00. The 132-column
 * print files become in-memory report records that the batch tasklets log and
 * the REST layer can return.
 */
@Service
public class ReportService {

    /** RPTPOS00 WS-POSITION-DETAIL. */
    public record PositionLine(String portfolioId, String description, BigDecimal quantity,
                               BigDecimal currentValue, BigDecimal changePct) {
    }

    public record PositionReport(LocalDate reportDate, List<PositionLine> lines, BigDecimal totalValue) {
    }

    /** RPTAUD00 audit + error summaries. */
    public record AuditReport(LocalDateTime from, LocalDateTime to, long auditRecords,
                              Map<AuditStatus, Long> byStatus, long errorRecords, long severeErrors) {
    }

    /** RPTSTA00 batch statistics (DB2 stats replaced by Actuator metrics). */
    public record StatisticsReport(long positions, long historyRowsToday, long errorsLogged, long returnCodesLogged) {
    }

    /** RTNANA00 PRGCUR row: per-program success/warning/error/severe counts. */
    public record ReturnCodeLine(String programId, long total, long success, long warning, long error, long severe) {
    }

    public record ReturnCodeReport(List<ReturnCodeLine> lines, ReturnCodeLine totals) {
    }

    private final PositionRepository positionRepository;
    private final AuditLogRepository auditLogRepository;
    private final ErrorLogRepository errorLogRepository;
    private final PositionHistoryRepository positionHistoryRepository;
    private final ReturnCodeLogRepository returnCodeLogRepository;

    public ReportService(PositionRepository positionRepository, AuditLogRepository auditLogRepository,
                         ErrorLogRepository errorLogRepository, PositionHistoryRepository positionHistoryRepository,
                         ReturnCodeLogRepository returnCodeLogRepository) {
        this.positionRepository = positionRepository;
        this.auditLogRepository = auditLogRepository;
        this.errorLogRepository = errorLogRepository;
        this.positionHistoryRepository = positionHistoryRepository;
        this.returnCodeLogRepository = returnCodeLogRepository;
    }

    /** RPTPOS00 2100-PROCESS-POSITIONS / 2110-FORMAT-POSITION. */
    @Transactional(readOnly = true)
    public PositionReport positionReport() {
        List<PositionLine> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO.setScale(ScaleConverter.MONEY_SCALE);
        for (Position p : positionRepository.findAll()) {
            lines.add(new PositionLine(p.getId().getPortfolioId(), p.getDescription(), p.getQuantity(),
                    p.getMarketValue(), changePercent(p.getMarketValue(), p.getPreviousValue())));
            total = total.add(p.getMarketValue());
        }
        return new PositionReport(LocalDate.now(), lines, total);
    }

    /**
     * {@code COMPUTE WS-POS-CHANGE-PCT = (POS-CURRENT-VALUE - POS-PREVIOUS-VALUE) / POS-PREVIOUS-VALUE * 100}.
     * Target picture is +ZZ9.99 (2 decimals). COBOL raises SIZE ERROR on a zero divisor; we report 0.00.
     */
    public static BigDecimal changePercent(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.signum() == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return current.subtract(previous)
                .divide(previous, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** RPTAUD00 2100/2200. */
    @Transactional(readOnly = true)
    public AuditReport auditReport(LocalDateTime from, LocalDateTime to) {
        List<AuditLog> audits = auditLogRepository.findByTimestampBetweenOrderByTimestampAsc(from, to);
        Map<AuditStatus, Long> byStatus = new EnumMap<>(AuditStatus.class);
        for (AuditLog a : audits) {
            byStatus.merge(a.getStatus(), 1L, Long::sum);
        }
        List<ErrorLog> errors = errorLogRepository.findAll();
        long severe = errors.stream().filter(e -> e.getSeverity() >= 3).count();
        return new AuditReport(from, to, audits.size(), byStatus, errors.size(), severe);
    }

    /** RPTSTA00 2200-PROCESS-BATCH-STATS. */
    @Transactional(readOnly = true)
    public StatisticsReport statisticsReport() {
        return new StatisticsReport(positionRepository.count(),
                positionHistoryRepository.countByProcessDate(LocalDate.now()),
                errorLogRepository.count(), returnCodeLogRepository.count());
    }

    /** RTNANA00 PRGCUR grouping + 2300 totals line. */
    @Transactional(readOnly = true)
    public ReturnCodeReport returnCodeReport() {
        Map<String, long[]> byProgram = new TreeMap<>();
        for (ReturnCodeLog rc : returnCodeLogRepository.findAll()) {
            long[] counts = byProgram.computeIfAbsent(rc.getId().getProgramId(), k -> new long[5]);
            counts[0]++;
            switch (rc.getStatusCode()) {
                case "S" -> counts[1]++;
                case "W" -> counts[2]++;
                case "E" -> counts[3]++;
                case "F" -> counts[4]++;
                default -> {
                }
            }
        }
        List<ReturnCodeLine> lines = new ArrayList<>();
        long[] totals = new long[5];
        byProgram.forEach((program, c) -> {
            lines.add(new ReturnCodeLine(program, c[0], c[1], c[2], c[3], c[4]));
            for (int i = 0; i < 5; i++) {
                totals[i] += c[i];
            }
        });
        return new ReturnCodeReport(lines,
                new ReturnCodeLine("TOTALS", totals[0], totals[1], totals[2], totals[3], totals[4]));
    }
}
