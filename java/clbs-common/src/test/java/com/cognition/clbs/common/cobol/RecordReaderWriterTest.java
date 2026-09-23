package com.cognition.clbs.common.cobol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class RecordReaderWriterTest {

  @Test
  void writerAndReaderAgreeOnEveryFieldKind() {
    byte[] bytes =
        new RecordWriter(24, CobolCharset.ASCII)
            .alnum("AB", 4)
            .display(42, 6)
            .packed(new BigDecimal("-12.34"), 5, 2)
            .comp(-7, 4, true)
            .comp(70000, 9, false)
            .filler(5)
            .toBytes();

    assertThat(bytes).hasSize(24);
    assertThat(new String(bytes, 0, 10, StandardCharsets.US_ASCII)).isEqualTo("AB  000042");
    assertThat(new String(bytes, 19, 5, StandardCharsets.US_ASCII)).isEqualTo("     ");

    record Parsed(String a, int d, BigDecimal p, int c, int u) {}
    Parsed parsed =
        RecordReader.parse(
            bytes,
            CobolCharset.ASCII,
            24,
            in -> {
              Parsed result =
                  new Parsed(
                      in.alnum(4),
                      in.display(6),
                      in.packed(5, 2),
                      in.comp(4, true),
                      in.comp(9, false));
              in.skip(5);
              return result;
            });

    assertThat(parsed).isEqualTo(new Parsed("AB", 42, new BigDecimal("-12.34"), -7, 70000));
  }

  @Test
  void ebcdicIsSupportedForCharacterFields() {
    byte[] bytes =
        new RecordWriter(6, CobolCharset.EBCDIC).alnum("PORT", 4).display(7, 2).toBytes();

    assertThat(bytes[0]).isEqualTo((byte) 0xD7); // 'P' in IBM-1047
    assertThat(bytes[4]).isEqualTo((byte) 0xF0); // '0'
    assertThat(
            RecordReader.<String>parse(
                bytes, CobolCharset.EBCDIC, 6, in -> in.alnum(4) + in.display(2)))
        .isEqualTo("PORT7");
  }

  @Test
  void readerRejectsWrongLengthAndIncompleteConsumption() {
    byte[] bytes = new byte[10];
    assertThatThrownBy(() -> RecordReader.parse(bytes, CobolCharset.ASCII, 12, in -> null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("copybook defines 12");
    assertThatThrownBy(() -> RecordReader.parse(bytes, CobolCharset.ASCII, 10, in -> in.alnum(4)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("consumed 4 of 10");
    assertThatThrownBy(() -> RecordReader.parse(bytes, CobolCharset.ASCII, 10, in -> in.alnum(11)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("exceeds record");
    assertThatThrownBy(
            () -> RecordReader.parse(bytes, CobolCharset.ASCII, 10, in -> in.compLong(10, true)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("consumed 8 of 10");
  }

  @Test
  void readerHandlesDisplayEdgeCases() {
    byte[] blank = "        ".getBytes(StandardCharsets.US_ASCII);
    assertThat(RecordReader.<Integer>parse(blank, CobolCharset.ASCII, 8, in -> in.display(8)))
        .isZero();

    byte[] bad = "12A4".getBytes(StandardCharsets.US_ASCII);
    assertThatThrownBy(() -> RecordReader.parse(bad, CobolCharset.ASCII, 4, in -> in.display(4)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Non-numeric");
    assertThatThrownBy(
            () -> RecordReader.parse(bad, CobolCharset.ASCII, 4, in -> in.comp(10, true)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("compLong");
  }

  @Test
  void readerExposesPositionAndLongComp() {
    byte[] bytes = new RecordWriter(8, CobolCharset.ASCII).comp(1L << 40, 18, true).toBytes();
    long value =
        RecordReader.parse(
            bytes,
            CobolCharset.ASCII,
            8,
            in -> {
              assertThat(in.position()).isZero();
              long v = in.compLong(18, true);
              assertThat(in.position()).isEqualTo(8);
              return v;
            });
    assertThat(value).isEqualTo(1L << 40);
  }

  @Test
  void writerTruncatesLongTextAndTreatsNullAsSpaces() {
    byte[] bytes =
        new RecordWriter(6, CobolCharset.ASCII).alnum("ABCDEFGH", 4).alnum(null, 2).toBytes();
    assertThat(new String(bytes, StandardCharsets.US_ASCII)).isEqualTo("ABCD  ");
  }

  @Test
  void writerWritesNullPackedAsZero() {
    byte[] bytes = new RecordWriter(3, CobolCharset.ASCII).packed(null, 5, 2).toBytes();
    assertThat(bytes).containsExactly(0x00, 0x00, 0x0C);
  }

  @Test
  void writerRejectsOverflowAndPartialRecords() {
    assertThatThrownBy(() -> new RecordWriter(4, CobolCharset.ASCII).display(-1, 4))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new RecordWriter(4, CobolCharset.ASCII).display(10000, 4))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new RecordWriter(4, CobolCharset.ASCII).alnum("x", 5))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("exceeds record");
    assertThatThrownBy(() -> new RecordWriter(4, CobolCharset.ASCII).alnum("x", 2).toBytes())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("only 2 were written");
    assertThatThrownBy(() -> new RecordWriter(4, StandardCharsets.UTF_16))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
