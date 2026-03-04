package com.cobolbenchmark.batch;

import com.cobolbenchmark.common.BatchConstants;
import com.cobolbenchmark.db.BatchControlRepository;
import com.cobolbenchmark.model.BatchControlRecord;
import com.cobolbenchmark.model.BatchStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RecoveryProcessJob - from RCVPRC00.
 * Tests recovery decision logic (restart/bypass/terminate).
 */
@ExtendWith(MockitoExtension.class)
class RecoveryProcessJobTest {

    @Mock
    private BatchControlRepository batchControlRepository;

    private RecoveryProcessJob recoveryProcessJob;

    @BeforeEach
    void setUp() {
        recoveryProcessJob = new RecoveryProcessJob(batchControlRepository);
    }

    @Test
    void testDetermineAction_completedJob_bypass() {
        BatchControlRecord record = createRecord(BatchStatus.DONE.getCode(), 0, 0, 3);
        RecoveryProcessJob.RecoveryAction action = recoveryProcessJob.determineAction(record);
        assertEquals(RecoveryProcessJob.RecoveryAction.BYPASS, action);
    }

    @Test
    void testDetermineAction_maxRestartsExceeded_terminate() {
        BatchControlRecord record = createRecord(BatchStatus.ERROR.getCode(), 4, 3, 3);
        RecoveryProcessJob.RecoveryAction action = recoveryProcessJob.determineAction(record);
        assertEquals(RecoveryProcessJob.RecoveryAction.TERMINATE, action);
    }

    @Test
    void testDetermineAction_severeReturnCode_terminate() {
        BatchControlRecord record = createRecord(BatchStatus.ERROR.getCode(), 12, 0, 3);
        RecoveryProcessJob.RecoveryAction action = recoveryProcessJob.determineAction(record);
        assertEquals(RecoveryProcessJob.RecoveryAction.TERMINATE, action);
    }

    @Test
    void testDetermineAction_criticalReturnCode_terminate() {
        BatchControlRecord record = createRecord(BatchStatus.ERROR.getCode(), 16, 0, 3);
        RecoveryProcessJob.RecoveryAction action = recoveryProcessJob.determineAction(record);
        assertEquals(RecoveryProcessJob.RecoveryAction.TERMINATE, action);
    }

    @Test
    void testDetermineAction_recoverableError_restart() {
        BatchControlRecord record = createRecord(BatchStatus.ERROR.getCode(), 4, 1, 3);
        RecoveryProcessJob.RecoveryAction action = recoveryProcessJob.determineAction(record);
        assertEquals(RecoveryProcessJob.RecoveryAction.RESTART, action);
    }

    @Test
    void testDetermineAction_warningReturnCode_restart() {
        BatchControlRecord record = createRecord(BatchStatus.ERROR.getCode(), 2, 0, 3);
        RecoveryProcessJob.RecoveryAction action = recoveryProcessJob.determineAction(record);
        assertEquals(RecoveryProcessJob.RecoveryAction.RESTART, action);
    }

    @Test
    void testDetermineAction_zeroReturnCodeButNotDone_restart() {
        BatchControlRecord record = createRecord(BatchStatus.ACTIVE.getCode(), 0, 0, 3);
        RecoveryProcessJob.RecoveryAction action = recoveryProcessJob.determineAction(record);
        assertEquals(RecoveryProcessJob.RecoveryAction.RESTART, action);
    }

    private BatchControlRecord createRecord(String status, int returnCode, int restartCount, int maxRestarts) {
        BatchControlRecord record = new BatchControlRecord();
        record.setJobName("TESTJOB");
        record.setProcessDate("20260304");
        record.setSequenceNo(1);
        record.setStatus(status);
        record.setReturnCode(returnCode);
        record.setRestartCount(restartCount);
        record.setMaxRestarts(maxRestarts);
        return record;
    }
}
