package com.cognition.clbs.common.copybook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cognition.clbs.common.copybook.batch.BatchControlRecord;
import com.cognition.clbs.common.copybook.batch.BatchControlRecord.Prerequisite;
import com.cognition.clbs.common.copybook.batch.CheckpointControl;
import com.cognition.clbs.common.copybook.batch.CheckpointControl.FileStatus;
import com.cognition.clbs.common.copybook.batch.CheckpointRecord;
import com.cognition.clbs.common.copybook.batch.ProcessSequenceRecord;
import com.cognition.clbs.common.copybook.batch.ProcessSequenceRecord.Dependency;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class BatchCopybookRoundTripTest {

  @Test
  void batchControlRecordMatchesBchctlCopybook() {
    BatchControlRecord record =
        new BatchControlRecord(
            "POSUPD00",
            "20240315",
            20,
            "A",
            "STEP010",
            "POSUPDT",
            "02000000",
            "",
            2,
            List.of(new Prerequisite("TRNVAL00", 10, 0), new Prerequisite("INITDAY", 1, 4)),
            0,
            "",
            1,
            SampleRecords.TIMESTAMP,
            "");

    assertThat(record.prerequisites()).hasSize(BatchControlRecord.PREREQ_OCCURS);
    assertThat(record.prerequisites().get(2)).isEqualTo(Prerequisite.EMPTY);
    byte[] bytes = record.toBytes();
    assertThat(bytes).hasSize(381);
    assertThat(BatchControlRecord.parse(bytes)).isEqualTo(record);
    assertThatThrownBy(
            () ->
                new BatchControlRecord(
                    "J",
                    "",
                    0,
                    "",
                    "",
                    "",
                    "",
                    "",
                    0,
                    Collections.nCopies(11, Prerequisite.EMPTY),
                    0,
                    "",
                    0,
                    "",
                    ""))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void checkpointControlMatchesCkprstCopybook() {
    CheckpointControl control =
        new CheckpointControl(
            "POSUPDT",
            "20240315",
            "143045",
            "A",
            123_456_789L,
            123_456_000L,
            789L,
            1,
            "PORT0001" + "20240315" + "AAPL",
            SampleRecords.TIMESTAMP,
            "20",
            List.of(new FileStatus("TRANFILE", "PORT000120240315", "00")),
            CheckpointControl.DEFAULT_COMMIT_FREQUENCY,
            CheckpointControl.DEFAULT_MAX_ERRORS,
            CheckpointControl.DEFAULT_MAX_RESTARTS,
            "R");

    assertThat(control.files()).hasSize(CheckpointControl.FILE_OCCURS);
    byte[] bytes = control.toBytes();
    assertThat(bytes).hasSize(424);
    assertThat(CheckpointControl.parse(bytes)).isEqualTo(control);
    assertThat(CheckpointControl.Phase.fromCode("20")).isEqualTo(CheckpointControl.Phase.PROCESS);
    assertThat(CheckpointControl.Status.fromCode("R"))
        .isEqualTo(CheckpointControl.Status.RESTARTED);
    assertThat(CheckpointControl.RestartMode.fromCode("C"))
        .isEqualTo(CheckpointControl.RestartMode.RECOVER);
  }

  @Test
  void checkpointRecordMatchesCkprstCopybook() {
    CheckpointRecord record = new CheckpointRecord("POSUPDT", "20240315", "opaque state");
    byte[] bytes = record.toBytes();
    assertThat(bytes).hasSize(416);
    assertThat(CheckpointRecord.KEY_LENGTH).isEqualTo(16);
    assertThat(CheckpointRecord.parse(bytes)).isEqualTo(record);
  }

  @Test
  void processSequenceRecordMatchesPrcseqCopybook() {
    ProcessSequenceRecord record =
        new ProcessSequenceRecord(
            "POSUPD00",
            1,
            "Position update",
            "PRC",
            "D",
            200,
            120,
            2,
            List.of(new Dependency("TRNVAL00", "H", 4), new Dependency("INITDAY", "S", 8)),
            "POSUPDT",
            "MODE=FULL",
            4,
            "Y",
            ProcessSequenceRecord.WEEKDAYS,
            "N",
            "N",
            "POSRCVR",
            "",
            100,
            "2024-01-01",
            "SYSADMIN",
            "2024-03-15",
            "SYSADMIN");

    assertThat(record.dependencies()).hasSize(ProcessSequenceRecord.DEPENDENCY_OCCURS);
    byte[] bytes = record.toBytes();
    assertThat(bytes).hasSize(380);
    assertThat(ProcessSequenceRecord.parse(bytes)).isEqualTo(record);
    assertThat(ProcessSequenceRecord.Type.fromCode("PRC"))
        .isEqualTo(ProcessSequenceRecord.Type.PROCESS);
    assertThat(ProcessSequenceRecord.Frequency.fromCode("M"))
        .isEqualTo(ProcessSequenceRecord.Frequency.MONTHLY);
    assertThat(ProcessSequenceRecord.DependencyType.fromCode("H"))
        .isEqualTo(ProcessSequenceRecord.DependencyType.HARD);
    assertThat(ProcessSequenceRecord.STANDARD_SEQUENCES).hasSize(3);
  }
}
