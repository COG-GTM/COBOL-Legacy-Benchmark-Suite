package com.clbs.utility;

import com.clbs.common.Inputs;
import com.clbs.common.ProgramResult;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * UTLMON00 — system monitoring utility.
 *
 * <p>The COBOL source defines the CONFIG-RECORD layout, the resource / threshold / alert-level
 * vocabularies and the monitoring cycle (collect, check thresholds, log, alert, sleep, repeat
 * until WS-HOUR = 23). All metric collection paragraphs (2110-2140) and all threshold paragraphs
 * (2210-2240) are PERFORMed but never defined, and the metrics come from z/OS SMF and a DB2STATS
 * dataset that do not exist here, so this runs a single cycle over the supplied metrics instead of
 * inventing metric sources or an hour-long loop.
 */
@Service
public class SystemMonitorUtility {

    public static final String PROGRAM = "UTLMON00";

    public static final String CPU = "CPU";
    public static final String MEMORY = "MEMORY";
    public static final String DASD = "DASD";
    public static final String DB2 = "DB2";

    public static final String UTIL = "UTIL";
    public static final String RESPONSE = "RESPONSE";
    public static final String QUEUE = "QUEUE";
    public static final String ERROR = "ERROR";

    public static final String INFO = "INFO";
    public static final String WARNING = "WARNING";
    public static final String CRITICAL = "CRITICAL";

    /** CONFIG-RECORD. */
    public record MonitorConfig(String resourceType, String thresholdType,
            BigDecimal thresholdValue, String alertLevel, String alertAction) {
    }

    /** One observed metric supplied in place of the undefined 2110-2140 collectors. */
    public record Metric(String resourceType, String thresholdType, BigDecimal value) {
    }

    /** ALERT-RECORD. */
    public record Alert(String level, String resource, String message) {
    }

    /** The MONITOR-LOG lines plus any ALERT-FILE records for one cycle. */
    public record MonitorCycle(ProgramResult result, List<Alert> alerts) {
    }

    /** One pass of 2000-PROCESS. */
    public MonitorCycle cycle(List<MonitorConfig> configs, List<Metric> metrics) {
        ProgramResult result = new ProgramResult(PROGRAM);
        List<Alert> alerts = new ArrayList<>();
        List<MonitorConfig> configured = Inputs.records(configs);
        List<Metric> collected = Inputs.records(metrics);

        for (Metric metric : collected) {
            MonitorConfig config = configured.stream()
                    .filter(candidate -> candidate.resourceType().equals(metric.resourceType())
                            && candidate.thresholdType().equals(metric.thresholdType()))
                    .findFirst()
                    .orElse(null);

            boolean breached = config != null
                    && metric.value().compareTo(config.thresholdValue()) > 0;
            result.display(String.format("%-10s %-20s %12s %-10s", metric.resourceType(),
                    metric.thresholdType(), metric.value(), breached ? "BREACH" : "OK"));

            if (breached) {
                alerts.add(new Alert(config.alertLevel(), config.resourceType(),
                        config.alertAction()));
            }
        }

        result.count("metrics", collected.size()).count("alerts", alerts.size());
        return new MonitorCycle(result, alerts);
    }
}
