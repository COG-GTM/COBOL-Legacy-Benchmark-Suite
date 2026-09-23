package com.cognition.clbs.common.copybook.common;

import com.cognition.clbs.common.cobol.CobolCode;

/**
 * Constant tables from {@code ERRHAND.cpy}: {@code STD-ERROR-CODES}, {@code ERR-CATEGORIES}, {@code
 * ERR-VSAM-STATUSES} and {@code ERR-VSAM-MSGS}. {@code ERR-RETURN-CODES} is {@link ReturnCode}.
 */
public final class ErrorCodes {

  private ErrorCodes() {}

  /** {@code STD-ERROR-CODES}: the {@code PIC X(4)} application error codes. */
  public enum Standard implements CobolCode {
    INVALID_DATA("E001"),
    NOT_FOUND("E002"),
    DUPLICATE("E003"),
    FILE_ERROR("E004"),
    DB_ERROR("E005"),
    SECURITY("E006"),
    PROCESSING("E007"),
    VALIDATION("E008"),
    VERSION("E009"),
    TIMEOUT("E010");

    private final String code;

    Standard(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }

    /** Resolves a standard error code. */
    public static Standard fromCode(String code) {
      return CobolCode.fromCode(Standard.class, code);
    }
  }

  /** {@code ERR-CATEGORIES}: the {@code PIC X(2)} error categories. */
  public enum Category implements CobolCode {
    VSAM("VS"),
    VALIDATION("VL"),
    PROCESSING("PR"),
    SYSTEM("SY");

    private final String code;

    Category(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }

    /** Resolves an error category. */
    public static Category fromCode(String code) {
      return CobolCode.fromCode(Category.class, code);
    }
  }

  /** {@code ERR-VSAM-STATUSES} with their {@code ERR-VSAM-MSGS} text. */
  public enum VsamStatus implements CobolCode {
    SUCCESS("00", ""),
    END_OF_FILE("10", ""),
    DUPLICATE_KEY("22", "Duplicate record key"),
    NOT_FOUND("23", "Record not found");

    /** {@code ERR-OTHER}: message for any status not listed here. */
    public static final String OTHER_MESSAGE = "Unexpected VSAM error";

    private final String code;
    private final String message;

    VsamStatus(String code, String message) {
      this.code = code;
      this.message = message;
    }

    @Override
    public String code() {
      return code;
    }

    /** The copybook message text for this status (empty when none is defined). */
    public String message() {
      return message;
    }

    /** Resolves a two-byte VSAM file status. */
    public static VsamStatus fromCode(String code) {
      return CobolCode.fromCode(VsamStatus.class, code);
    }

    /** Message for an arbitrary file status, falling back to {@link #OTHER_MESSAGE}. */
    public static String messageFor(String fileStatus) {
      for (VsamStatus status : values()) {
        if (status.code.equals(fileStatus) && !status.message.isEmpty()) {
          return status.message;
        }
      }
      return OTHER_MESSAGE;
    }
  }
}
