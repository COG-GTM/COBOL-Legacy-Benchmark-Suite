package com.cognition.clbs.common.copybook.batch;

import com.cognition.clbs.common.cobol.CobolCharset;
import com.cognition.clbs.common.cobol.CopybookRecord;
import com.cognition.clbs.common.cobol.RecordReader;
import com.cognition.clbs.common.cobol.RecordWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code BATCH-CONTROL-RECORD} from {@code BCHCTL.cpy}: one job-step run in the BCHCTL VSAM file
 * (381 bytes, key = job + process date + sequence).
 *
 * @param jobName {@code BCT-JOB-NAME PIC X(8)}
 * @param processDate {@code BCT-PROCESS-DATE PIC X(8)} as {@code YYYYMMDD}
 * @param sequenceNumber {@code BCT-SEQUENCE-NO PIC 9(4)}
 * @param status {@code BCT-STATUS PIC X(1)}, see {@link BatchConstants.Status}
 * @param stepName {@code BCT-STEP-NAME PIC X(8)}
 * @param programName {@code BCT-PROGRAM-NAME PIC X(8)}
 * @param startTime {@code BCT-START-TIME PIC X(8)}
 * @param endTime {@code BCT-END-TIME PIC X(8)}
 * @param prerequisiteCount {@code BCT-PREREQ-COUNT PIC 9(2) COMP}
 * @param prerequisites {@code BCT-PREREQ-JOBS OCCURS 10 TIMES}, always exactly ten entries
 * @param returnCode {@code BCT-RETURN-CODE PIC S9(4) COMP}
 * @param errorDescription {@code BCT-ERROR-DESC PIC X(80)}
 * @param restartCount {@code BCT-RESTART-COUNT PIC 9(2) COMP}
 * @param attemptTimestamp {@code BCT-ATTEMPT-TS PIC X(26)}
 * @param completeTimestamp {@code BCT-COMPLETE-TS PIC X(26)}
 */
public record BatchControlRecord(
    String jobName,
    String processDate,
    int sequenceNumber,
    String status,
    String stepName,
    String programName,
    String startTime,
    String endTime,
    int prerequisiteCount,
    List<Prerequisite> prerequisites,
    int returnCode,
    String errorDescription,
    int restartCount,
    String attemptTimestamp,
    String completeTimestamp)
    implements CopybookRecord {

  /** Record length in bytes. */
  public static final int LENGTH = 381;

  /** Key length in bytes ({@code BCT-KEY}). */
  public static final int KEY_LENGTH = 20;

  /** {@code OCCURS} count of {@code BCT-PREREQ-JOBS}. */
  public static final int PREREQ_OCCURS = BatchConstants.MAX_PREREQ;

  /** Canonical constructor: pads or rejects the prerequisite table to exactly ten entries. */
  public BatchControlRecord {
    prerequisites = padTable(prerequisites, PREREQ_OCCURS, Prerequisite.EMPTY);
  }

  /** Parses a GnuCOBOL (ASCII) record. */
  public static BatchControlRecord parse(byte[] bytes) {
    return parse(bytes, CobolCharset.ASCII);
  }

  /** Parses a record in the given encoding. */
  public static BatchControlRecord parse(byte[] bytes, Charset charset) {
    return RecordReader.parse(bytes, charset, LENGTH, BatchControlRecord::read);
  }

  /** Reads the fields in copybook order from {@code in}. */
  public static BatchControlRecord read(RecordReader in) {
    String jobName = in.alnum(8);
    String processDate = in.alnum(8);
    int sequenceNumber = in.display(4);
    String status = in.alnum(1);
    String stepName = in.alnum(8);
    String programName = in.alnum(8);
    String startTime = in.alnum(8);
    String endTime = in.alnum(8);
    int prerequisiteCount = in.comp(2, false);
    List<Prerequisite> prerequisites = new ArrayList<>(PREREQ_OCCURS);
    for (int i = 0; i < PREREQ_OCCURS; i++) {
      prerequisites.add(Prerequisite.read(in));
    }
    int returnCode = in.comp(4, true);
    String errorDescription = in.alnum(80);
    int restartCount = in.comp(2, false);
    String attemptTimestamp = in.alnum(26);
    String completeTimestamp = in.alnum(26);
    in.skip(50);
    return new BatchControlRecord(
        jobName,
        processDate,
        sequenceNumber,
        status,
        stepName,
        programName,
        startTime,
        endTime,
        prerequisiteCount,
        prerequisites,
        returnCode,
        errorDescription,
        restartCount,
        attemptTimestamp,
        completeTimestamp);
  }

  /** Copies {@code table} into an immutable list of exactly {@code occurs} entries. */
  static <T> List<T> padTable(List<T> table, int occurs, T empty) {
    List<T> source = table == null ? List.of() : table;
    if (source.size() > occurs) {
      throw new IllegalArgumentException(
          "Table has " + source.size() + " entries, copybook allows " + occurs);
    }
    List<T> padded = new ArrayList<>(occurs);
    padded.addAll(source);
    while (padded.size() < occurs) {
      padded.add(empty);
    }
    return List.copyOf(padded);
  }

  @Override
  public int recordLength() {
    return LENGTH;
  }

  @Override
  public void writeTo(RecordWriter out) {
    out.alnum(jobName, 8)
        .alnum(processDate, 8)
        .display(sequenceNumber, 4)
        .alnum(status, 1)
        .alnum(stepName, 8)
        .alnum(programName, 8)
        .alnum(startTime, 8)
        .alnum(endTime, 8)
        .comp(prerequisiteCount, 2, false);
    for (Prerequisite prerequisite : prerequisites) {
      prerequisite.writeTo(out);
    }
    out.comp(returnCode, 4, true)
        .alnum(errorDescription, 80)
        .comp(restartCount, 2, false)
        .alnum(attemptTimestamp, 26)
        .alnum(completeTimestamp, 26)
        .filler(50);
  }

  /**
   * One {@code BCT-PREREQ-JOBS} entry (14 bytes).
   *
   * @param name {@code BCT-PREREQ-NAME PIC X(8)}
   * @param sequenceNumber {@code BCT-PREREQ-SEQ PIC 9(4)}
   * @param returnCode {@code BCT-PREREQ-RC PIC S9(4) COMP}
   */
  public record Prerequisite(String name, int sequenceNumber, int returnCode) {

    /** An unused table slot: spaces, zero, zero. */
    public static final Prerequisite EMPTY = new Prerequisite("", 0, 0);

    static Prerequisite read(RecordReader in) {
      return new Prerequisite(in.alnum(8), in.display(4), in.comp(4, true));
    }

    void writeTo(RecordWriter out) {
      out.alnum(name, 8).display(sequenceNumber, 4).comp(returnCode, 4, true);
    }
  }
}
