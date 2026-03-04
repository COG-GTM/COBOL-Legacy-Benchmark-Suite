package com.cobolbenchmark.batch;

import com.cobolbenchmark.db.BatchControlRepository;
import com.cobolbenchmark.db.ProcessSequenceRepository;
import com.cobolbenchmark.model.BatchControlRecord;
import com.cobolbenchmark.model.BatchStatus;
import com.cobolbenchmark.model.Dependency;
import com.cobolbenchmark.model.DependencyType;
import com.cobolbenchmark.model.ProcessSequenceRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ProcessSequenceJob - from PRCSEQ00.
 * Tests dependency checking logic (hard/soft dependencies).
 */
@ExtendWith(MockitoExtension.class)
class ProcessSequenceJobTest {

    @Mock
    private ProcessSequenceRepository processSequenceRepository;

    @Mock
    private BatchControlRepository batchControlRepository;

    private ProcessSequenceJob processSequenceJob;

    @BeforeEach
    void setUp() {
        processSequenceJob = new ProcessSequenceJob(processSequenceRepository, batchControlRepository);
    }

    @Test
    void testCheckDependencies_noDependencies() {
        ProcessSequenceRecord process = new ProcessSequenceRecord();
        process.setProcessId("PROC001");
        process.setDependencies(new ArrayList<>());

        boolean result = processSequenceJob.checkDependencies(process, "20260304");
        assertTrue(result, "Process with no dependencies should pass");
    }

    @Test
    void testCheckDependencies_hardDependencySatisfied() {
        ProcessSequenceRecord process = new ProcessSequenceRecord();
        process.setProcessId("PROC002");

        Dependency dep = new Dependency("PROC002", 1, "PROC001", DependencyType.HARD, 4);
        process.setDependencies(List.of(dep));

        BatchControlRecord depRecord = new BatchControlRecord();
        depRecord.setJobName("PROC001");
        depRecord.setStatus(BatchStatus.DONE.getCode());
        depRecord.setReturnCode(0);

        when(batchControlRepository.findByJobNameAndProcessDate("PROC001", "20260304"))
                .thenReturn(List.of(depRecord));

        boolean result = processSequenceJob.checkDependencies(process, "20260304");
        assertTrue(result, "Hard dependency should be satisfied when job is DONE with RC=0");
    }

    @Test
    void testCheckDependencies_hardDependencyNotSatisfied() {
        ProcessSequenceRecord process = new ProcessSequenceRecord();
        process.setProcessId("PROC002");

        Dependency dep = new Dependency("PROC002", 1, "PROC001", DependencyType.HARD, 4);
        process.setDependencies(List.of(dep));

        BatchControlRecord depRecord = new BatchControlRecord();
        depRecord.setJobName("PROC001");
        depRecord.setStatus(BatchStatus.ERROR.getCode());
        depRecord.setReturnCode(8);

        when(batchControlRepository.findByJobNameAndProcessDate("PROC001", "20260304"))
                .thenReturn(List.of(depRecord));

        boolean result = processSequenceJob.checkDependencies(process, "20260304");
        assertFalse(result, "Hard dependency should fail when job RC exceeds threshold");
    }

    @Test
    void testCheckDependencies_softDependencyNotSatisfied() {
        ProcessSequenceRecord process = new ProcessSequenceRecord();
        process.setProcessId("PROC002");

        Dependency dep = new Dependency("PROC002", 1, "PROC001", DependencyType.SOFT, 4);
        process.setDependencies(List.of(dep));

        when(batchControlRepository.findByJobNameAndProcessDate("PROC001", "20260304"))
                .thenReturn(Collections.emptyList());

        boolean result = processSequenceJob.checkDependencies(process, "20260304");
        assertTrue(result, "Soft dependency should not block process execution");
    }

    @Test
    void testCheckDependencies_hardDependencyNotFound() {
        ProcessSequenceRecord process = new ProcessSequenceRecord();
        process.setProcessId("PROC002");

        Dependency dep = new Dependency("PROC002", 1, "PROC001", DependencyType.HARD, 4);
        process.setDependencies(List.of(dep));

        when(batchControlRepository.findByJobNameAndProcessDate("PROC001", "20260304"))
                .thenReturn(Collections.emptyList());

        boolean result = processSequenceJob.checkDependencies(process, "20260304");
        assertFalse(result, "Hard dependency should fail when prerequisite job not found");
    }
}
