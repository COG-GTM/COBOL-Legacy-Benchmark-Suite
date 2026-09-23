package com.cognition.clbs.common.copybook.batch;

import com.cognition.clbs.common.cobol.CobolCharset;
import com.cognition.clbs.common.cobol.CopybookRecord;
import com.cognition.clbs.common.cobol.RecordReader;
import com.cognition.clbs.common.cobol.RecordWriter;
import java.nio.charset.Charset;

/**
 * {@code CHECKPOINT-RECORD} from {@code CKPRST.cpy}: the persisted CKPTFILE record (416 bytes, key
 * = program + run date). {@code CKR-DATA} carries an opaque 400-byte checkpoint image.
 *
 * @param programId {@code CKR-PROGRAM-ID PIC X(8)}
 * @param runDate {@code CKR-RUN-DATE PIC X(8)} as {@code YYYYMMDD}
 * @param data {@code CKR-DATA PIC X(400)}
 */
public record CheckpointRecord(String programId, String runDate, String data)
    implements CopybookRecord {

  /** Record length in bytes. */
  public static final int LENGTH = 416;

  /** Key length in bytes ({@code CKR-KEY}). */
  public static final int KEY_LENGTH = 16;

  /** Width of {@code CKR-DATA}. */
  public static final int DATA_LENGTH = 400;

  /** Parses a GnuCOBOL (ASCII) record. */
  public static CheckpointRecord parse(byte[] bytes) {
    return parse(bytes, CobolCharset.ASCII);
  }

  /** Parses a record in the given encoding. */
  public static CheckpointRecord parse(byte[] bytes, Charset charset) {
    return RecordReader.parse(bytes, charset, LENGTH, CheckpointRecord::read);
  }

  /** Reads the fields in copybook order from {@code in}. */
  public static CheckpointRecord read(RecordReader in) {
    return new CheckpointRecord(in.alnum(8), in.alnum(8), in.alnum(DATA_LENGTH));
  }

  @Override
  public int recordLength() {
    return LENGTH;
  }

  @Override
  public void writeTo(RecordWriter out) {
    out.alnum(programId, 8).alnum(runDate, 8).alnum(data, DATA_LENGTH);
  }
}
