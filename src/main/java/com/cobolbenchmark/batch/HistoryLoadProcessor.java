package com.cobolbenchmark.batch;

import com.cobolbenchmark.model.PoshistRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * History Load Processor - migrated from HISTLD00.cbl processing logic.
 * Validates and transforms records before DB2 insert.
 */
@Component
public class HistoryLoadProcessor implements ItemProcessor<PoshistRecord, PoshistRecord> {

    private static final Logger logger = LoggerFactory.getLogger(HistoryLoadProcessor.class);

    @Override
    public PoshistRecord process(PoshistRecord item) throws Exception {
        // Set processing timestamps
        if (item.getProcessDate() == null) {
            item.setProcessDate(LocalDate.now());
        }
        if (item.getProcessTime() == null) {
            item.setProcessTime(LocalTime.now());
        }
        if (item.getTransDate() == null) {
            item.setTransDate(LocalDate.now());
        }
        if (item.getTransTime() == null) {
            item.setTransTime(LocalTime.now());
        }
        if (item.getAuditTimestamp() == null) {
            item.setAuditTimestamp(new java.sql.Timestamp(System.currentTimeMillis()));
        }

        logger.debug("Processing history record: account={} portfolio={}",
                item.getAccountNo(), item.getPortfolioId());

        return item;
    }
}
