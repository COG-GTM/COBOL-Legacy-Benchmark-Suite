package com.cognition.clbs.common.copybook.batch;

import com.cognition.clbs.common.cobol.CobolCharset;
import com.cognition.clbs.common.cobol.CobolCode;
import com.cognition.clbs.common.cobol.CopybookRecord;
import com.cognition.clbs.common.cobol.RecordReader;
import com.cognition.clbs.common.cobol.RecordWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code CHECKPOINT-CONTROL} from {@code CKPRST.cpy}: the in-memory checkpoint/restart control area
 * that CKPRST serialises into {@link CheckpointRecord#data()} (424 bytes).
 *
 * @param programId {@code CK-PROGRAM-ID PIC X(8)}
 * @param runDate {@code CK-RUN-DATE PIC X(8)}
 * @param runTime {@code CK-RUN-TIME PIC X(6)}
 * @param status {@code CK-STATUS PIC X(1)}, see {@link Status}
 * @param recordsRead {@code CK-RECORDS-READ PIC 9(9) COMP}
 * @param recordsProcessed {@code CK-RECORDS-PROC PIC 9(9) COMP}
 * @param recordsInError {@code CK-RECORDS-ERROR PIC 9(9) COMP}
 * @param restartCount {@code CK-RESTART-COUNT PIC 9(2) COMP}
 * @param lastKey {@code CK-LAST-KEY PIC X(50)}
 * @param lastTime {@code CK-LAST-TIME PIC X(26)}
 * @param phase {@code CK-PHASE PIC X(2)}, see {@link Phase}
 * @param files {@code CK-FILE-STATUS OCCURS 5 TIMES}, always exactly five entries
 * @param commitFrequency {@code CK-COMMIT-FREQ PIC 9(5) COMP VALUE 1000}
 * @param maxErrors {@code CK-MAX-ERRORS PIC 9(3) COMP VALUE 100}
 * @param maxRestarts {@code CK-MAX-RESTARTS PIC 9(2) COMP VALUE 3}
 * @param restartMode {@code CK-RESTART-MODE PIC X(1)}, see {@link RestartMode}
 */
public record CheckpointControl(
    String programId,
    String runDate,
    String runTime,
    String status,
    long recordsRead,
    long recordsProcessed,
    long recordsInError,
    int restartCount,
    String lastKey,
    String lastTime,
    String phase,
    List<FileStatus> files,
    int commitFrequency,
    int maxErrors,
    int maxRestarts,
    String restartMode)
    implements CopybookRecord {

  /** Record length in bytes. */
  public static final int LENGTH = 424;

  /** {@code OCCURS} count of {@code CK-FILE-STATUS}. */
  public static final int FILE_OCCURS = 5;

  /** Copybook default for {@code CK-COMMIT-FREQ}. */
  public static final int DEFAULT_COMMIT_FREQUENCY = 1000;

  /** Copybook default for {@code CK-MAX-ERRORS}. */
  public static final int DEFAULT_MAX_ERRORS = 100;

  /** Copybook default for {@code CK-MAX-RESTARTS}. */
  public static final int DEFAULT_MAX_RESTARTS = 3;

  /** Canonical constructor: pads or rejects the file table to exactly five entries. */
  public CheckpointControl {
    files = BatchControlRecord.padTable(files, FILE_OCCURS, FileStatus.EMPTY);
  }

  /** Parses a GnuCOBOL (ASCII) record. */
  public static CheckpointControl parse(byte[] bytes) {
    return parse(bytes, CobolCharset.ASCII);
  }

  /** Parses a record in the given encoding. */
  public static CheckpointControl parse(byte[] bytes, Charset charset) {
    return RecordReader.parse(bytes, charset, LENGTH, CheckpointControl::read);
  }

  /** Reads the fields in copybook order from {@code in}. */
  public static CheckpointControl read(RecordReader in) {
    String programId = in.alnum(8);
    String runDate = in.alnum(8);
    String runTime = in.alnum(6);
    String status = in.alnum(1);
    long recordsRead = in.compLong(9, false);
    long recordsProcessed = in.compLong(9, false);
    long recordsInError = in.compLong(9, false);
    int restartCount = in.comp(2, false);
    String lastKey = in.alnum(50);
    String lastTime = in.alnum(26);
    String phase = in.alnum(2);
    List<FileStatus> files = new ArrayList<>(FILE_OCCURS);
    for (int i = 0; i < FILE_OCCURS; i++) {
      files.add(FileStatus.read(in));
    }
    int commitFrequency = in.comp(5, false);
    int maxErrors = in.comp(3, false);
    int maxRestarts = in.comp(2, false);
    String restartMode = in.alnum(1);
    return new CheckpointControl(
        programId,
        runDate,
        runTime,
        status,
        recordsRead,
        recordsProcessed,
        recordsInError,
        restartCount,
        lastKey,
        lastTime,
        phase,
        files,
        commitFrequency,
        maxErrors,
        maxRestarts,
        restartMode);
  }

  @Override
  public int recordLength() {
    return LENGTH;
  }

  @Override
  public void writeTo(RecordWriter out) {
    out.alnum(programId, 8)
        .alnum(runDate, 8)
        .alnum(runTime, 6)
        .alnum(status, 1)
        .comp(recordsRead, 9, false)
        .comp(recordsProcessed, 9, false)
        .comp(recordsInError, 9, false)
        .comp(restartCount, 2, false)
        .alnum(lastKey, 50)
        .alnum(lastTime, 26)
        .alnum(phase, 2);
    for (FileStatus file : files) {
      file.writeTo(out);
    }
    out.comp(commitFrequency, 5, false)
        .comp(maxErrors, 3, false)
        .comp(maxRestarts, 2, false)
        .alnum(restartMode, 1);
  }

  /** {@code CK-STATUS} condition names. */
  public enum Status implements CobolCode {
    INITIAL("I"),
    ACTIVE("A"),
    COMPLETE("C"),
    FAILED("F"),
    RESTARTED("R");

    private final String code;

    Status(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }

    /** Resolves a checkpoint status. */
    public static Status fromCode(String code) {
      return CobolCode.fromCode(Status.class, code);
    }
  }

  /** {@code CK-PHASE} condition names. */
  public enum Phase implements CobolCode {
    INIT("00"),
    READ("10"),
    PROCESS("20"),
    UPDATE("30"),
    TERMINATE("40");

    private final String code;

    Phase(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }

    /** Resolves a processing phase. */
    public static Phase fromCode(String code) {
      return CobolCode.fromCode(Phase.class, code);
    }
  }

  /** {@code CK-RESTART-MODE} condition names. */
  public enum RestartMode implements CobolCode {
    NORMAL("N"),
    RESTART("R"),
    RECOVER("C");

    private final String code;

    RestartMode(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }

    /** Resolves a restart mode. */
    public static RestartMode fromCode(String code) {
      return CobolCode.fromCode(RestartMode.class, code);
    }
  }

  /**
   * One {@code CK-FILE-STATUS} entry (60 bytes).
   *
   * @param fileName {@code CK-FILE-NAME PIC X(8)}
   * @param position {@code CK-FILE-POS PIC X(50)}
   * @param fileStatus {@code CK-FILE-STATUS PIC X(2)}
   */
  public record FileStatus(String fileName, String position, String fileStatus) {

    /** An unused table slot. */
    public static final FileStatus EMPTY = new FileStatus("", "", "");

    static FileStatus read(RecordReader in) {
      return new FileStatus(in.alnum(8), in.alnum(50), in.alnum(2));
    }

    void writeTo(RecordWriter out) {
      out.alnum(fileName, 8).alnum(position, 50).alnum(fileStatus, 2);
    }
  }
}
