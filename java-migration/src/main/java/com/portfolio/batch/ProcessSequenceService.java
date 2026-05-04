package com.portfolio.batch;

import com.portfolio.domain.BatchConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Process Sequence Service - migrated from COBOL PRCSEQ00.cbl.
 * Manages process sequencing and dependency checking.
 * BCT-PREREQ-JOBS dependency checking -> Spring Batch step flow.
 */
@Service
public class ProcessSequenceService {

    private static final Logger log = LoggerFactory.getLogger(ProcessSequenceService.class);

    private final BatchControlService batchControlService;

    public ProcessSequenceService(BatchControlService batchControlService) {
        this.batchControlService = batchControlService;
    }

    public List<String> getMainProcessSequence() {
        List<String> sequence = new ArrayList<>();
        sequence.add("TRNVAL00");
        sequence.add("POSUPD00");
        sequence.add("HISTLD00");
        return sequence;
    }

    public List<String> getStartOfDaySequence() {
        List<String> sequence = new ArrayList<>();
        sequence.add("INITDAY");
        sequence.add("CKPCLR");
        sequence.add("DATEVAL");
        return sequence;
    }

    public List<String> getEndOfDaySequence() {
        List<String> sequence = new ArrayList<>();
        sequence.add("RPTGEN00");
        sequence.add("BCKLOD00");
        sequence.add("ENDDAY");
        return sequence;
    }

    public boolean canExecute(String jobName) {
        return batchControlService.checkPrerequisites(jobName, LocalDate.now());
    }
}
