package com.clbs.api;

import com.clbs.batch.AuditReportProgram;
import com.clbs.batch.BatchControlProcessor;
import com.clbs.batch.HistoryLoadProgram;
import com.clbs.batch.PositionReportProgram;
import com.clbs.batch.ProcessSequenceManager;
import com.clbs.batch.RecoveryProcessor;
import com.clbs.batch.ReturnCodeAnalysisProgram;
import com.clbs.batch.StatisticsReportProgram;
import com.clbs.common.ProgramResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** HTTP entry points for the batch programs, replacing the JCL job steps. */
@RestController
@RequestMapping("/api/batch")
public class BatchController {

    private final BatchControlProcessor control;
    private final ProcessSequenceManager sequence;
    private final RecoveryProcessor recovery;
    private final HistoryLoadProgram historyLoad;
    private final PositionReportProgram positionReport;
    private final AuditReportProgram auditReport;
    private final StatisticsReportProgram statisticsReport;
    private final ReturnCodeAnalysisProgram returnCodeAnalysis;

    public BatchController(BatchControlProcessor control, ProcessSequenceManager sequence,
            RecoveryProcessor recovery, HistoryLoadProgram historyLoad,
            PositionReportProgram positionReport, AuditReportProgram auditReport,
            StatisticsReportProgram statisticsReport,
            ReturnCodeAnalysisProgram returnCodeAnalysis) {
        this.control = control;
        this.sequence = sequence;
        this.recovery = recovery;
        this.historyLoad = historyLoad;
        this.positionReport = positionReport;
        this.auditReport = auditReport;
        this.statisticsReport = statisticsReport;
        this.returnCodeAnalysis = returnCodeAnalysis;
    }

    /** BCHCTL00. */
    @PostMapping("/control")
    public int control(@RequestBody BatchControlProcessor.ControlRequest request) {
        return control.execute(request);
    }

    /** PRCSEQ00 initialization. */
    @PostMapping("/sequence")
    public ProcessSequenceManager.Session sequence(@RequestParam String processDate,
            @RequestParam String sequenceType) {
        return sequence.initialize(processDate, sequenceType);
    }

    /** RCVPRC00. */
    @PostMapping("/recovery")
    public ProgramResult recover(@RequestBody RecoveryProcessor.RecoveryRequest request) {
        return recovery.recover(request);
    }

    /** HISTLD00. */
    @PostMapping("/history-load")
    public ProgramResult historyLoad(@RequestParam String processDate) {
        return historyLoad.run(processDate);
    }

    /** RPTPOS00. */
    @GetMapping("/reports/positions")
    public ProgramResult positionReport() {
        return positionReport.run();
    }

    /** RPTAUD00. */
    @GetMapping("/reports/audit")
    public ProgramResult auditReport() {
        return auditReport.run();
    }

    /** RPTSTA00. */
    @GetMapping("/reports/statistics")
    public ProgramResult statisticsReport() {
        return statisticsReport.run();
    }

    /** RTNANA00. */
    @GetMapping("/reports/return-codes")
    public ProgramResult returnCodeAnalysis() {
        return returnCodeAnalysis.run();
    }
}
