package com.cognition.clbs.common.cobol;

/**
 * An enum constant that stands in for a COBOL 88-level condition name or a {@code VALUE} literal.
 * {@link #code()} is the exact literal stored in the record.
 */
public interface CobolCode {

  /** The literal as it appears in the copybook {@code VALUE} clause. */
  String code();

  /** Resolves {@code code} against the constants of {@code type}, failing on unknown values. */
  static <E extends Enum<E> & CobolCode> E fromCode(Class<E> type, String code) {
    for (E constant : type.getEnumConstants()) {
      if (constant.code().equals(code)) {
        return constant;
      }
    }
    throw new IllegalArgumentException("Unknown " + type.getSimpleName() + " code '" + code + "'");
  }
}
