package com.clbs.portfolio.batch.control;

import com.clbs.portfolio.exception.BatchProcessingException;
import com.clbs.portfolio.model.BatchControlRecord;
import com.clbs.portfolio.model.BatchControlRecord.BatchControlStatus;
import com.clbs.portfolio.model.BatchControlRecordId;
import com.clbs.portfolio.repository.BatchControlRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BatchControlProcessorTest {

    @Mock
    private BatchControlRecordRepository repository;

    private BatchControlProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new BatchControlProcessor(repository);
    }

    @Test
    void initializeShouldSetStatusToActive() {
        BatchControlRecord record = BatchControlRecord.builder()
                .jobName("TRNVAL00")
                .processDate("20240320")
                .sequenceNo(1)
                .status(BatchControlStatus.READY)
                .build();

        when(repository.findById(any(BatchControlRecordId.class))).thenReturn(Optional.of(record));
        when(repository.save(any(BatchControlRecord.class))).thenReturn(record);

        processor.initialize("TRNVAL00", "20240320", 1);

        ArgumentCaptor<BatchControlRecord> captor = ArgumentCaptor.forClass(BatchControlRecord.class);
        verify(repository).save(captor.capture());
        assertEquals(BatchControlStatus.ACTIVE, captor.getValue().getStatus());
        assertNotNull(captor.getValue().getAttemptTimestamp());
    }

    @Test
    void initializeShouldThrowWhenNotReady() {
        BatchControlRecord record = BatchControlRecord.builder()
                .jobName("TRNVAL00")
                .processDate("20240320")
                .sequenceNo(1)
                .status(BatchControlStatus.DONE)
                .build();

        when(repository.findById(any(BatchControlRecordId.class))).thenReturn(Optional.of(record));

        assertThrows(BatchProcessingException.class, () ->
                processor.initialize("TRNVAL00", "20240320", 1));
    }

    @Test
    void initializeShouldThrowWhenRecordNotFound() {
        when(repository.findById(any(BatchControlRecordId.class))).thenReturn(Optional.empty());

        assertThrows(BatchProcessingException.class, () ->
                processor.initialize("NOTFOUND", "20240320", 1));
    }

    @Test
    void checkPrerequisitesShouldReturnTrueWhenNoPrereqs() {
        BatchControlRecord record = BatchControlRecord.builder()
                .jobName("TRNVAL00")
                .processDate("20240320")
                .sequenceNo(1)
                .status(BatchControlStatus.ACTIVE)
                .build();

        when(repository.findById(any(BatchControlRecordId.class))).thenReturn(Optional.of(record));

        assertTrue(processor.checkPrerequisites("TRNVAL00", "20240320", 1));
    }

    @Test
    void terminateShouldSetDoneForSuccessfulReturn() {
        BatchControlRecord record = BatchControlRecord.builder()
                .jobName("TRNVAL00")
                .processDate("20240320")
                .sequenceNo(1)
                .status(BatchControlStatus.ACTIVE)
                .startTime("08000000")
                .build();

        when(repository.findById(any(BatchControlRecordId.class))).thenReturn(Optional.of(record));
        when(repository.save(any(BatchControlRecord.class))).thenReturn(record);

        processor.terminate("TRNVAL00", "20240320", 1, 0);

        ArgumentCaptor<BatchControlRecord> captor = ArgumentCaptor.forClass(BatchControlRecord.class);
        verify(repository).save(captor.capture());
        assertEquals(BatchControlStatus.DONE, captor.getValue().getStatus());
        assertEquals(0, captor.getValue().getReturnCode());
        assertNotNull(captor.getValue().getCompleteTimestamp());
    }

    @Test
    void terminateShouldSetErrorForHighReturnCode() {
        BatchControlRecord record = BatchControlRecord.builder()
                .jobName("TRNVAL00")
                .processDate("20240320")
                .sequenceNo(1)
                .status(BatchControlStatus.ACTIVE)
                .build();

        when(repository.findById(any(BatchControlRecordId.class))).thenReturn(Optional.of(record));
        when(repository.save(any(BatchControlRecord.class))).thenReturn(record);

        processor.terminate("TRNVAL00", "20240320", 1, 8);

        ArgumentCaptor<BatchControlRecord> captor = ArgumentCaptor.forClass(BatchControlRecord.class);
        verify(repository).save(captor.capture());
        assertEquals(BatchControlStatus.ERROR, captor.getValue().getStatus());
    }
}
