package com.cognition.clbs.common.cobol;

import java.nio.charset.Charset;

/**
 * A Java record that mirrors a COBOL 01-level record layout byte for byte. Implementations expose a
 * {@code LENGTH} constant, a static {@code parse(byte[])} factory and {@link #writeTo}, so the same
 * type can be round-tripped against GnuCOBOL golden data.
 */
public interface CopybookRecord {

  /** Total record length in bytes, as declared by the copybook. */
  int recordLength();

  /** Writes every field, including FILLER, in copybook order. */
  void writeTo(RecordWriter out);

  /** Serialises the record using the GnuCOBOL baseline encoding. */
  default byte[] toBytes() {
    return toBytes(CobolCharset.ASCII);
  }

  /** Serialises the record using the given single-byte encoding. */
  default byte[] toBytes(Charset charset) {
    RecordWriter out = new RecordWriter(recordLength(), charset);
    writeTo(out);
    return out.toBytes();
  }
}
