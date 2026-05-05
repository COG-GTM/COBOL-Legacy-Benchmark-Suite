package com.portfolio.portmstr.batch;

import com.portfolio.portmstr.batch.checkpoint.CheckpointService;
import com.portfolio.portmstr.model.BatchCheckpoint;
import com.portfolio.portmstr.model.enums.CheckpointStatus;
import com.portfolio.portmstr.repository.BatchCheckpointRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for CheckpointService.
 * Verifies checkpoint/restart mechanism matches COBOL CKPRST.cbl behavior:
 *   PROC-INIT, PROC-TAKE-CHECKPOINT, PROC-COMMIT-CHECKPOINT, PROC-RESTART
 */
@ExtendWith(MockitoExtension.class)
class CheckpointServiceTest {

    @Mock
    private BatchCheckpointRepository checkpointRepository;

    @InjectMocks
    private CheckpointService checkpointService;

    private static final String PROGRAM_ID = "PORTBAT";

    @Nested
    @DisplayName("PROC-INIT Tests")
    class InitializeCheckpointTests {

        @Test
        @DisplayName("Creates new checkpoint for fresh run")
        void initializeCheckpoint_newRun() {
            ReflectionTestUtils.setField(checkpointService, "maxRestarts", 3);
            when(checkpointRepository.findByProgramIdAndRunDate(PROGRAM_ID, LocalDate.now()))
                    .thenReturn(Optional.empty());
            when(checkpointRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            BatchCheckpoint result = checkpointService.initializeCheckpoint(PROGRAM_ID);

            assertEquals(PROGRAM_ID, result.getProgramId());
            assertEquals(CheckpointStatus.INITIAL, result.getStatus());
            assertEquals(0L, result.getRecordsRead());
            assertEquals(0L, result.getRecordsProcessed());
            assertEquals(0L, result.getRecordsError());
            assertEquals(0, result.getRestartCount());
            assertEquals("00", result.getPhase());
        }

        @Test
        @DisplayName("Restarts from failed checkpoint")
        void initializeCheckpoint_restart() {
            ReflectionTestUtils.setField(checkpointService, "maxRestarts", 3);

            BatchCheckpoint failed = new BatchCheckpoint();
            failed.setProgramId(PROGRAM_ID);
            failed.setRunDate(LocalDate.now());
            failed.setStatus(CheckpointStatus.FAILED);
            failed.setRestartCount(1);
            failed.setRecordsRead(500L);
            failed.setRecordsProcessed(490L);
            failed.setRecordsError(10L);
            failed.setLastKey("PORT0500");

            when(checkpointRepository.findByProgramIdAndRunDate(PROGRAM_ID, LocalDate.now()))
                    .thenReturn(Optional.of(failed));
            when(checkpointRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            BatchCheckpoint result = checkpointService.initializeCheckpoint(PROGRAM_ID);

            assertEquals(CheckpointStatus.RESTARTED, result.getStatus());
            assertEquals(2, result.getRestartCount());
        }

        @Test
        @DisplayName("Fails when max restarts exceeded (CK-MAX-RESTARTS)")
        void initializeCheckpoint_maxRestartsExceeded() {
            ReflectionTestUtils.setField(checkpointService, "maxRestarts", 3);

            BatchCheckpoint failed = new BatchCheckpoint();
            failed.setProgramId(PROGRAM_ID);
            failed.setRunDate(LocalDate.now());
            failed.setStatus(CheckpointStatus.FAILED);
            failed.setRestartCount(3);

            when(checkpointRepository.findByProgramIdAndRunDate(PROGRAM_ID, LocalDate.now()))
                    .thenReturn(Optional.of(failed));
            when(checkpointRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            BatchCheckpoint result = checkpointService.initializeCheckpoint(PROGRAM_ID);

            assertEquals(CheckpointStatus.FAILED, result.getStatus());
        }
    }

    @Nested
    @DisplayName("PROC-TAKE-CHECKPOINT Tests")
    class TakeCheckpointTests {

        @Test
        @DisplayName("Takes checkpoint with current processing state")
        void takeCheckpoint_success() {
            BatchCheckpoint existing = new BatchCheckpoint();
            existing.setProgramId(PROGRAM_ID);
            existing.setRunDate(LocalDate.now());
            existing.setStatus(CheckpointStatus.INITIAL);

            when(checkpointRepository.findByProgramIdAndRunDate(PROGRAM_ID, LocalDate.now()))
                    .thenReturn(Optional.of(existing));
            when(checkpointRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            BatchCheckpoint result = checkpointService.takeCheckpoint(
                    PROGRAM_ID, "PORT0100", 100, 95, 5, "20");

            assertEquals(CheckpointStatus.ACTIVE, result.getStatus());
            assertEquals("PORT0100", result.getLastKey());
            assertEquals(100L, result.getRecordsRead());
            assertEquals(95L, result.getRecordsProcessed());
            assertEquals(5L, result.getRecordsError());
            assertEquals("20", result.getPhase());
        }
    }

    @Nested
    @DisplayName("PROC-COMMIT-CHECKPOINT Tests")
    class CompleteCheckpointTests {

        @Test
        @DisplayName("Marks checkpoint as complete with terminal phase")
        void completeCheckpoint_success() {
            BatchCheckpoint active = new BatchCheckpoint();
            active.setProgramId(PROGRAM_ID);
            active.setRunDate(LocalDate.now());
            active.setStatus(CheckpointStatus.ACTIVE);

            when(checkpointRepository.findByProgramIdAndRunDate(PROGRAM_ID, LocalDate.now()))
                    .thenReturn(Optional.of(active));
            when(checkpointRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            BatchCheckpoint result = checkpointService.completeCheckpoint(PROGRAM_ID);

            assertEquals(CheckpointStatus.COMPLETE, result.getStatus());
            assertEquals("40", result.getPhase());
        }
    }

    @Nested
    @DisplayName("Error Threshold Tests")
    class ErrorThresholdTests {

        @Test
        @DisplayName("Continues processing below max errors (CK-MAX-ERRORS)")
        void shouldContinue_belowThreshold() {
            ReflectionTestUtils.setField(checkpointService, "maxErrors", 100);
            assertTrue(checkpointService.shouldContinueProcessing(50));
        }

        @Test
        @DisplayName("Stops processing at max errors")
        void shouldStop_atThreshold() {
            ReflectionTestUtils.setField(checkpointService, "maxErrors", 100);
            assertFalse(checkpointService.shouldContinueProcessing(100));
        }

        @Test
        @DisplayName("Stops processing above max errors")
        void shouldStop_aboveThreshold() {
            ReflectionTestUtils.setField(checkpointService, "maxErrors", 100);
            assertFalse(checkpointService.shouldContinueProcessing(150));
        }
    }

    @Nested
    @DisplayName("Commit Frequency Tests")
    class CommitFrequencyTests {

        @Test
        @DisplayName("Checkpoint at commit frequency interval (CK-COMMIT-FREQ)")
        void shouldCheckpoint_atFrequency() {
            ReflectionTestUtils.setField(checkpointService, "commitFrequency", 1000);
            assertTrue(checkpointService.shouldTakeCheckpoint(1000));
            assertTrue(checkpointService.shouldTakeCheckpoint(2000));
        }

        @Test
        @DisplayName("No checkpoint between intervals")
        void noCheckpoint_betweenIntervals() {
            ReflectionTestUtils.setField(checkpointService, "commitFrequency", 1000);
            assertFalse(checkpointService.shouldTakeCheckpoint(500));
            assertFalse(checkpointService.shouldTakeCheckpoint(1500));
        }

        @Test
        @DisplayName("No checkpoint at zero records")
        void noCheckpoint_atZero() {
            ReflectionTestUtils.setField(checkpointService, "commitFrequency", 1000);
            assertFalse(checkpointService.shouldTakeCheckpoint(0));
        }
    }

    @Nested
    @DisplayName("PROC-RESTART Tests")
    class RestartInfoTests {

        @Test
        @DisplayName("Returns restart info for restarted checkpoint")
        void getRestartInfo_restarted() {
            BatchCheckpoint restarted = new BatchCheckpoint();
            restarted.setProgramId(PROGRAM_ID);
            restarted.setStatus(CheckpointStatus.RESTARTED);
            restarted.setLastKey("PORT0500");

            when(checkpointRepository.findTopByProgramIdOrderByRunDateDesc(PROGRAM_ID))
                    .thenReturn(Optional.of(restarted));

            Optional<BatchCheckpoint> result = checkpointService.getRestartInfo(PROGRAM_ID);

            assertTrue(result.isPresent());
            assertEquals("PORT0500", result.get().getLastKey());
        }

        @Test
        @DisplayName("Returns empty for completed checkpoint")
        void getRestartInfo_completed() {
            BatchCheckpoint completed = new BatchCheckpoint();
            completed.setProgramId(PROGRAM_ID);
            completed.setStatus(CheckpointStatus.COMPLETE);

            when(checkpointRepository.findTopByProgramIdOrderByRunDateDesc(PROGRAM_ID))
                    .thenReturn(Optional.of(completed));

            Optional<BatchCheckpoint> result = checkpointService.getRestartInfo(PROGRAM_ID);

            assertFalse(result.isPresent());
        }
    }
}
