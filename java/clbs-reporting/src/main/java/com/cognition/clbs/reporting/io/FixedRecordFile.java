package com.cognition.clbs.reporting.io;

import com.cognition.clbs.common.cobol.CobolCharset;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Reads and writes fixed-length record files (the sequential-file view of the VSAM data sets the
 * COBOL report programs read): no record delimiters, every record exactly {@code recordLength}
 * bytes.
 */
public final class FixedRecordFile {

  private FixedRecordFile() {}

  /** Reads every record of {@code path} and decodes it with {@code parser}. */
  public static <T> List<T> readAll(
      Path path, int recordLength, BiFunction<byte[], Charset, T> parser) throws IOException {
    return readAll(path, recordLength, CobolCharset.ASCII, parser);
  }

  /** Reads every record of {@code path} in {@code charset} and decodes it with {@code parser}. */
  public static <T> List<T> readAll(
      Path path, int recordLength, Charset charset, BiFunction<byte[], Charset, T> parser)
      throws IOException {
    byte[] bytes = Files.readAllBytes(path);
    if (bytes.length % recordLength != 0) {
      throw new IOException(
          path
              + " has "
              + bytes.length
              + " bytes, which is not a multiple of the record length "
              + recordLength);
    }
    List<T> records = new ArrayList<>(bytes.length / recordLength);
    for (int offset = 0; offset < bytes.length; offset += recordLength) {
      records.add(parser.apply(Arrays.copyOfRange(bytes, offset, offset + recordLength), charset));
    }
    return records;
  }
}
