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
 * {@code PROCESS-SEQUENCE-RECORD} from {@code PRCSEQ.cpy}: a scheduled process definition in the
 * PRCSEQ VSAM file (380 bytes, key = process id + version).
 *
 * @param processId {@code PSR-PROCESS-ID PIC X(8)}
 * @param version {@code PSR-VERSION PIC 9(2)}
 * @param description {@code PSR-DESCRIPTION PIC X(30)}
 * @param type {@code PSR-TYPE PIC X(3)}, see {@link Type}
 * @param frequency {@code PSR-FREQ PIC X(1)}, see {@link Frequency}
 * @param startTime {@code PSR-START-TIME PIC 9(4)} as {@code HHMM}
 * @param maxTime {@code PSR-MAX-TIME PIC 9(4)} in minutes
 * @param dependencyCount {@code PSR-DEP-COUNT PIC 9(2) COMP}
 * @param dependencies {@code PSR-DEP-ENTRY OCCURS 10 TIMES}, always exactly ten entries
 * @param program {@code PSR-PROGRAM PIC X(8)}
 * @param parameter {@code PSR-PARM PIC X(50)}
 * @param maxReturnCode {@code PSR-MAX-RC PIC S9(4) COMP}
 * @param restartable {@code PSR-RESTART PIC X(1)} ({@code Y}/{@code N})
 * @param activeDays {@code PSR-ACTIVE-DAYS PIC X(7)} Mon..Sun flags, e.g. {@code YYYYYNN}
 * @param monthEnd {@code PSR-MONTH-END PIC X(1)}
 * @param holidayRun {@code PSR-HOLIDAY-RUN PIC X(1)}
 * @param recoveryProgram {@code PSR-RECOVERY-PGM PIC X(8)}
 * @param recoveryParameter {@code PSR-RECOVERY-PARM PIC X(50)}
 * @param errorLimit {@code PSR-ERROR-LIMIT PIC 9(4) COMP}
 * @param createDate {@code PSR-CREATE-DATE PIC X(10)}
 * @param createUser {@code PSR-CREATE-USER PIC X(8)}
 * @param updateDate {@code PSR-UPDATE-DATE PIC X(10)}
 * @param updateUser {@code PSR-UPDATE-USER PIC X(8)}
 */
public record ProcessSequenceRecord(
    String processId,
    int version,
    String description,
    String type,
    String frequency,
    int startTime,
    int maxTime,
    int dependencyCount,
    List<Dependency> dependencies,
    String program,
    String parameter,
    int maxReturnCode,
    String restartable,
    String activeDays,
    String monthEnd,
    String holidayRun,
    String recoveryProgram,
    String recoveryParameter,
    int errorLimit,
    String createDate,
    String createUser,
    String updateDate,
    String updateUser)
    implements CopybookRecord {

  /** Record length in bytes. */
  public static final int LENGTH = 380;

  /** Key length in bytes ({@code PSR-KEY}). */
  public static final int KEY_LENGTH = 10;

  /** {@code OCCURS} count of {@code PSR-DEP-ENTRY}. */
  public static final int DEPENDENCY_OCCURS = 10;

  /** {@code PSR-WEEKDAY VALUE 'YYYYYNN'}. */
  public static final String WEEKDAYS = "YYYYYNN";

  /** {@code PSR-WEEKEND VALUE 'NNNNNYY'}. */
  public static final String WEEKEND = "NNNNNYY";

  /** {@code PSR-ALL-DAYS VALUE 'YYYYYYY'}. */
  public static final String ALL_DAYS = "YYYYYYY";

  /** {@code STANDARD-SEQUENCES}: the canonical start-of-day, main and end-of-day process orders. */
  public static final List<List<String>> STANDARD_SEQUENCES =
      List.of(
          List.of("INITDAY", "CKPCLR", "DATEVAL"),
          List.of("TRNVAL00", "POSUPD00", "HISTLD00"),
          List.of("RPTGEN00", "BCKLOD00", "ENDDAY"));

  /** Canonical constructor: pads or rejects the dependency table to exactly ten entries. */
  public ProcessSequenceRecord {
    dependencies = BatchControlRecord.padTable(dependencies, DEPENDENCY_OCCURS, Dependency.EMPTY);
  }

  /** Parses a GnuCOBOL (ASCII) record. */
  public static ProcessSequenceRecord parse(byte[] bytes) {
    return parse(bytes, CobolCharset.ASCII);
  }

  /** Parses a record in the given encoding. */
  public static ProcessSequenceRecord parse(byte[] bytes, Charset charset) {
    return RecordReader.parse(bytes, charset, LENGTH, ProcessSequenceRecord::read);
  }

  /** Reads the fields in copybook order from {@code in}. */
  public static ProcessSequenceRecord read(RecordReader in) {
    String processId = in.alnum(8);
    int version = in.display(2);
    String description = in.alnum(30);
    String type = in.alnum(3);
    String frequency = in.alnum(1);
    int startTime = in.display(4);
    int maxTime = in.display(4);
    int dependencyCount = in.comp(2, false);
    List<Dependency> dependencies = new ArrayList<>(DEPENDENCY_OCCURS);
    for (int i = 0; i < DEPENDENCY_OCCURS; i++) {
      dependencies.add(Dependency.read(in));
    }
    String program = in.alnum(8);
    String parameter = in.alnum(50);
    int maxReturnCode = in.comp(4, true);
    String restartable = in.alnum(1);
    String activeDays = in.alnum(7);
    String monthEnd = in.alnum(1);
    String holidayRun = in.alnum(1);
    String recoveryProgram = in.alnum(8);
    String recoveryParameter = in.alnum(50);
    int errorLimit = in.comp(4, false);
    String createDate = in.alnum(10);
    String createUser = in.alnum(8);
    String updateDate = in.alnum(10);
    String updateUser = in.alnum(8);
    in.skip(50);
    return new ProcessSequenceRecord(
        processId,
        version,
        description,
        type,
        frequency,
        startTime,
        maxTime,
        dependencyCount,
        dependencies,
        program,
        parameter,
        maxReturnCode,
        restartable,
        activeDays,
        monthEnd,
        holidayRun,
        recoveryProgram,
        recoveryParameter,
        errorLimit,
        createDate,
        createUser,
        updateDate,
        updateUser);
  }

  @Override
  public int recordLength() {
    return LENGTH;
  }

  @Override
  public void writeTo(RecordWriter out) {
    out.alnum(processId, 8)
        .display(version, 2)
        .alnum(description, 30)
        .alnum(type, 3)
        .alnum(frequency, 1)
        .display(startTime, 4)
        .display(maxTime, 4)
        .comp(dependencyCount, 2, false);
    for (Dependency dependency : dependencies) {
      dependency.writeTo(out);
    }
    out.alnum(program, 8)
        .alnum(parameter, 50)
        .comp(maxReturnCode, 4, true)
        .alnum(restartable, 1)
        .alnum(activeDays, 7)
        .alnum(monthEnd, 1)
        .alnum(holidayRun, 1)
        .alnum(recoveryProgram, 8)
        .alnum(recoveryParameter, 50)
        .comp(errorLimit, 4, false)
        .alnum(createDate, 10)
        .alnum(createUser, 8)
        .alnum(updateDate, 10)
        .alnum(updateUser, 8)
        .filler(50);
  }

  /** {@code PSR-TYPE} condition names. */
  public enum Type implements CobolCode {
    INIT("INI"),
    PROCESS("PRC"),
    REPORT("RPT"),
    TERMINATE("TRM");

    private final String code;

    Type(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }

    /** Resolves a process type. */
    public static Type fromCode(String code) {
      return CobolCode.fromCode(Type.class, code);
    }
  }

  /** {@code PSR-FREQ} condition names. */
  public enum Frequency implements CobolCode {
    DAILY("D"),
    WEEKLY("W"),
    MONTHLY("M");

    private final String code;

    Frequency(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }

    /** Resolves a run frequency. */
    public static Frequency fromCode(String code) {
      return CobolCode.fromCode(Frequency.class, code);
    }
  }

  /** {@code PSR-DEP-TYPE} condition names. */
  public enum DependencyType implements CobolCode {
    HARD("H"),
    SOFT("S");

    private final String code;

    DependencyType(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }

    /** Resolves a dependency type. */
    public static DependencyType fromCode(String code) {
      return CobolCode.fromCode(DependencyType.class, code);
    }
  }

  /**
   * One {@code PSR-DEP-ENTRY} (11 bytes).
   *
   * @param processId {@code PSR-DEP-ID PIC X(8)}
   * @param type {@code PSR-DEP-TYPE PIC X(1)}, see {@link DependencyType}
   * @param maxReturnCode {@code PSR-DEP-RC PIC S9(4) COMP}
   */
  public record Dependency(String processId, String type, int maxReturnCode) {

    /** An unused table slot. */
    public static final Dependency EMPTY = new Dependency("", "", 0);

    static Dependency read(RecordReader in) {
      return new Dependency(in.alnum(8), in.alnum(1), in.comp(4, true));
    }

    void writeTo(RecordWriter out) {
      out.alnum(processId, 8).alnum(type, 1).comp(maxReturnCode, 4, true);
    }
  }
}
