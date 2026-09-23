package com.cognition.clbs.common.cobol;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Character encodings used for {@code PIC X} and {@code PIC 9} DISPLAY fields. The GnuCOBOL
 * baseline in {@code src/} writes ASCII; {@link #EBCDIC} is available for data unloaded from z/OS.
 */
public final class CobolCharset {

  /** Encoding of the GnuCOBOL reference build (what golden files are recorded in). */
  public static final Charset ASCII = StandardCharsets.US_ASCII;

  /** IBM-1047 (Latin-1 EBCDIC), the z/OS default. */
  public static final Charset EBCDIC = Charset.forName("IBM1047");

  private CobolCharset() {}
}
