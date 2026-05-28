package com.clbs.portfolio.batch.control;

import com.clbs.portfolio.model.BatchControlRecord;
import com.clbs.portfolio.model.BatchControlRecord.BatchControlStatus;
import com.clbs.portfolio.model.ProcessDependency;
import com.clbs.portfolio.model.ProcessSequenceRecord;
import com.clbs.portfolio.model.ProcessSequenceRecord.Frequency;
import com.clbs.portfolio.model.ProcessSequenceRecord.SequenceType;
import com.clbs.portfolio.repository.BatchControlRecordRepository;
import com.clbs.portfolio.repository.ProcessSequenceRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessSequenceManagerTest {

    @Mock
    private ProcessSequenceRecordRepository processSequenceRecordRepository;

    @Mock
    private BatchControlRecordRepository batchControlRecordRepository;

    private ProcessSequenceManager manager;

    @BeforeEach
    void setUp() {
        manager = new ProcessSequenceManager(processSequenceRecordRepository, batchControlRecordRepository);
    }

    @Test
    void shouldReturnProcessesInOrder() {
        ProcessSequenceRecord p1 = ProcessSequenceRecord.builder()
                .processId("TRNVAL00")
                .version(1)
                .description("Transaction Validation")
                .type(SequenceType.PROCESS)
                .frequency(Frequency.DAILY)
                .startTime(800)
                .build();

        ProcessSequenceRecord p2 = ProcessSequenceRecord.builder()
                .processId("POSUPD00")
                .version(1)
                .description("Position Update")
                .type(SequenceType.PROCESS)
                .frequency(Frequency.DAILY)
                .startTime(900)
                .build();

        when(processSequenceRecordRepository.findByTypeOrderByStartTimeAsc(SequenceType.PROCESS))
                .thenReturn(List.of(p1, p2));

        manager.initializeSequence(SequenceType.PROCESS);

        Optional<ProcessSequenceRecord> first = manager.getNextProcess("20240320");
        assertTrue(first.isPresent());
        assertEquals("TRNVAL00", first.get().getProcessId());

        Optional<ProcessSequenceRecord> second = manager.getNextProcess("20240320");
        assertTrue(second.isPresent());
        assertEquals("POSUPD00", second.get().getProcessId());

        Optional<ProcessSequenceRecord> third = manager.getNextProcess("20240320");
        assertFalse(third.isPresent());
    }

    @Test
    void shouldSkipProcessWithUnmetHardDependency() {
        ProcessDependency dep = ProcessDependency.builder()
                .depId("PREREQ01")
                .depType("H")
                .depRc(4)
                .build();
        List<ProcessDependency> deps = new ArrayList<>();
        deps.add(dep);

        ProcessSequenceRecord process = ProcessSequenceRecord.builder()
                .processId("TRNVAL00")
                .version(1)
                .description("Transaction Validation")
                .type(SequenceType.PROCESS)
                .frequency(Frequency.DAILY)
                .startTime(800)
                .dependencies(deps)
                .build();

        when(processSequenceRecordRepository.findByTypeOrderByStartTimeAsc(SequenceType.PROCESS))
                .thenReturn(List.of(process));
        when(batchControlRecordRepository.findByJobNameAndProcessDate("PREREQ01", "20240320"))
                .thenReturn(Collections.emptyList());

        manager.initializeSequence(SequenceType.PROCESS);

        Optional<ProcessSequenceRecord> result = manager.getNextProcess("20240320");
        assertFalse(result.isPresent());
    }

    @Test
    void shouldProcessWhenHardDependencyIsMet() {
        ProcessDependency dep = ProcessDependency.builder()
                .depId("PREREQ01")
                .depType("H")
                .depRc(4)
                .build();
        List<ProcessDependency> deps = new ArrayList<>();
        deps.add(dep);

        ProcessSequenceRecord process = ProcessSequenceRecord.builder()
                .processId("TRNVAL00")
                .version(1)
                .description("Transaction Validation")
                .type(SequenceType.PROCESS)
                .frequency(Frequency.DAILY)
                .startTime(800)
                .dependencies(deps)
                .build();

        BatchControlRecord prereqDone = BatchControlRecord.builder()
                .jobName("PREREQ01")
                .processDate("20240320")
                .sequenceNo(1)
                .status(BatchControlStatus.DONE)
                .returnCode(0)
                .build();

        when(processSequenceRecordRepository.findByTypeOrderByStartTimeAsc(SequenceType.PROCESS))
                .thenReturn(List.of(process));
        when(batchControlRecordRepository.findByJobNameAndProcessDate("PREREQ01", "20240320"))
                .thenReturn(List.of(prereqDone));

        manager.initializeSequence(SequenceType.PROCESS);

        Optional<ProcessSequenceRecord> result = manager.getNextProcess("20240320");
        assertTrue(result.isPresent());
        assertEquals("TRNVAL00", result.get().getProcessId());
    }

    @Test
    void terminateSequenceShouldReturnTrueWhenAllComplete() {
        ProcessSequenceRecord process = ProcessSequenceRecord.builder()
                .processId("TRNVAL00")
                .version(1)
                .type(SequenceType.PROCESS)
                .frequency(Frequency.DAILY)
                .build();

        BatchControlRecord done = BatchControlRecord.builder()
                .jobName("TRNVAL00")
                .processDate("20240320")
                .sequenceNo(1)
                .status(BatchControlStatus.DONE)
                .returnCode(0)
                .build();

        when(processSequenceRecordRepository.findByTypeOrderByStartTimeAsc(SequenceType.PROCESS))
                .thenReturn(List.of(process));
        when(batchControlRecordRepository.findByJobNameAndProcessDate("TRNVAL00", "20240320"))
                .thenReturn(List.of(done));

        manager.initializeSequence(SequenceType.PROCESS);

        assertTrue(manager.terminateSequence("20240320"));
    }
}
