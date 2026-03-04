package com.cobolbenchmark.batch;

import com.cobolbenchmark.common.BatchConstants;
import com.cobolbenchmark.db.BatchControlRepository;
import com.cobolbenchmark.model.BatchControlRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Return Analysis Job - migrated from RTNANA00.cbl.
 * Analyzes return codes across batch jobs and generates summary.
 */
@Service
public class ReturnAnalysisJob {

    private static final Logger logger = LoggerFactory.getLogger(ReturnAnalysisJob.class);

    private final BatchControlRepository batchControlRepository;

    public ReturnAnalysisJob(BatchControlRepository batchControlRepository) {
        this.batchControlRepository = batchControlRepository;
    }

    /**
     * Analyze return codes for a process date.
     * Classifies all job return codes and generates summary.
     */
    public Map<String, Object> analyzeReturnCodes(String processDate) {
        logger.info("Analyzing return codes for date: {}", processDate);

        List<BatchControlRecord> records = batchControlRepository.findAll();

        int totalJobs = 0;
        int successCount = 0;
        int warningCount = 0;
        int errorCount = 0;
        int severeCount = 0;
        int highestRc = 0;

        for (BatchControlRecord record : records) {
            if (processDate != null && !processDate.equals(record.getProcessDate())) {
                continue;
            }
            totalJobs++;
            int rc = record.getReturnCode();
            if (rc > highestRc) {
                highestRc = rc;
            }

            BatchConstants.ReturnCode classification = BatchConstants.ReturnCode.classify(rc);
            switch (classification) {
                case SUCCESS:
                    successCount++;
                    break;
                case WARNING:
                    warningCount++;
                    break;
                case ERROR:
                    errorCount++;
                    break;
                case SEVERE:
                case CRITICAL:
                    severeCount++;
                    break;
            }
        }

        Map<String, Object> analysis = new LinkedHashMap<>();
        analysis.put("processDate", processDate);
        analysis.put("totalJobs", totalJobs);
        analysis.put("successCount", successCount);
        analysis.put("warningCount", warningCount);
        analysis.put("errorCount", errorCount);
        analysis.put("severeCount", severeCount);
        analysis.put("highestReturnCode", highestRc);
        analysis.put("overallClassification", BatchConstants.ReturnCode.classify(highestRc).name());

        logger.info("Return code analysis complete: {} jobs, highest RC={}", totalJobs, highestRc);
        return analysis;
    }
}
