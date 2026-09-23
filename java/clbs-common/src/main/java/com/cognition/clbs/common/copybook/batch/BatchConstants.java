package com.cognition.clbs.common.copybook.batch;

import com.cognition.clbs.common.cobol.CobolCode;

/**
 * {@code BATCH-CONTROL-CONSTANTS} from {@code BCHCON.cpy}. {@code BCT-RC-THRESHOLDS} is {@link
 * com.cognition.clbs.common.copybook.common.ReturnCode}.
 */
public final class BatchConstants {

  /** {@code BCT-MAX-PREREQ PIC 9(2) COMP VALUE 10}: also the OCCURS count in BCHCTL/PRCSEQ. */
  public static final int MAX_PREREQ = 10;

  /** {@code BCT-MAX-RESTARTS PIC 9(2) COMP VALUE 3}. */
  public static final int MAX_RESTARTS = 3;

  /** {@code BCT-WAIT-INTERVAL PIC 9(4) COMP VALUE 300} (seconds). */
  public static final int WAIT_INTERVAL_SECONDS = 300;

  /** {@code BCT-MAX-WAIT-TIME PIC 9(4) COMP VALUE 3600} (seconds). */
  public static final int MAX_WAIT_SECONDS = 3600;

  private BatchConstants() {}

  /** {@code BCT-STAT-VALUES} / {@code BCT-STATUS} 88-levels. */
  public enum Status implements CobolCode {
    READY("R"),
    ACTIVE("A"),
    WAITING("W"),
    DONE("D"),
    ERROR("E");

    private final String code;

    Status(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }

    /** Resolves a batch status code. */
    public static Status fromCode(String code) {
      return CobolCode.fromCode(Status.class, code);
    }
  }

  /** {@code BCT-PROC-TYPES}. */
  public enum ProcessType implements CobolCode {
    INITIAL("INI"),
    UPDATE("UPD"),
    REPORT("RPT"),
    CLEANUP("CLN");

    private final String code;

    ProcessType(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }

    /** Resolves a process type code. */
    public static ProcessType fromCode(String code) {
      return CobolCode.fromCode(ProcessType.class, code);
    }
  }

  /** {@code BCT-DEP-TYPES}. */
  public enum DependencyType implements CobolCode {
    REQUIRED("R"),
    OPTIONAL("O"),
    EXCLUSIVE("X");

    private final String code;

    DependencyType(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }

    /** Resolves a dependency type code. */
    public static DependencyType fromCode(String code) {
      return CobolCode.fromCode(DependencyType.class, code);
    }
  }

  /**
   * {@code BCT-PROC-NAMES}. {@code BCT-EMERGENCY} is declared {@code PIC X(8) VALUE 'EMERGENCY'};
   * COBOL truncates the literal, so the stored value is {@code EMERGENC}.
   */
  public enum ProcessName implements CobolCode {
    START_OF_DAY("STARTDAY"),
    END_OF_DAY("ENDDAY"),
    EMERGENCY("EMERGENC");

    private final String code;

    ProcessName(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }

    /** Resolves a process name (trailing spaces ignored). */
    public static ProcessName fromCode(String code) {
      return CobolCode.fromCode(ProcessName.class, code == null ? null : code.stripTrailing());
    }
  }

  /** {@code BCT-REC-TYPES}. */
  public enum RecordType implements CobolCode {
    CONTROL("C"),
    PROCESS("P"),
    DEPENDENCY("D"),
    HISTORY("H");

    private final String code;

    RecordType(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }

    /** Resolves a batch record type code. */
    public static RecordType fromCode(String code) {
      return CobolCode.fromCode(RecordType.class, code);
    }
  }

  /** {@code BCT-MESSAGES}: {@code PIC X(30)} literals, trailing spaces stripped. */
  public enum Message {
    STARTING("Process starting..."),
    COMPLETE("Process completed successfully"),
    FAILED("Process failed - check errors"),
    WAITING("Waiting for prerequisites");

    /** Width of every {@code BCT-MESSAGES} field. */
    public static final int LENGTH = 30;

    private final String text;

    Message(String text) {
      this.text = text;
    }

    /** Message text without the copybook padding. */
    public String text() {
      return text;
    }
  }
}
