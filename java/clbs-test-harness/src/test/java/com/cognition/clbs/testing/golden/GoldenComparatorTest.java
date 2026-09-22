package com.cognition.clbs.testing.golden;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class GoldenComparatorTest {

  @Nested
  class Text {

    @Test
    void identicalTextMatches() {
      ComparisonResult result = GoldenComparator.compareText("a\nb\n", "a\nb\n");

      assertThat(result.matches()).isTrue();
      assertThat(result.expectedCount()).isEqualTo(2);
      assertThat(result.report()).isEqualTo("Output matches golden file (2 lines)");
    }

    @Test
    void lineEndingsAreNormalisedByDefault() {
      assertThat(GoldenComparator.compareText("a\r\nb\r\n", "a\nb\n").matches()).isTrue();
      assertThat(GoldenComparator.compareText("a\rb", "a\nb").matches()).isTrue();
    }

    @Test
    void strictOptionsKeepLineEndingsSignificant() {
      ComparisonResult result =
          GoldenComparator.compareText("a\r\nb\r\n", "a\nb\n", TextOptions.strict());

      assertThat(result.matches()).isFalse();
      assertThat(result.totalDifferences()).isEqualTo(2);
    }

    @Test
    void trailingBlankLinesAreIgnoredByDefaultButNotStrict() {
      assertThat(GoldenComparator.compareText("a\n\n\n", "a").matches()).isTrue();
      assertThat(GoldenComparator.compareText("a\n\n\n", "a", TextOptions.strict()).matches())
          .isFalse();
    }

    @Test
    void trailingWhitespaceIsSignificantUnlessIgnored() {
      assertThat(GoldenComparator.compareText("REPORT   \n", "REPORT\n").matches()).isFalse();
      assertThat(
              GoldenComparator.compareText(
                      "REPORT   \n",
                      "REPORT\n",
                      TextOptions.defaults().ignoringTrailingWhitespace())
                  .matches())
          .isTrue();
    }

    @Test
    void reportsChangedMissingAndUnexpectedLines() {
      ComparisonResult result = GoldenComparator.compareText("one\ntwo\nthree\n", "one\n2\n");

      assertThat(result.totalDifferences()).isEqualTo(2);
      assertThat(result.reported())
          .containsExactly(Difference.of(2, "two", "2"), Difference.of(3, "three", null));
      assertThat(result.report())
          .contains("2 differing lines (expected 3, actual 2 lines)")
          .contains("line 2")
          .contains("expected: |two|")
          .contains("actual:   |2|")
          .contains("line 3 missing in actual")
          .contains("actual:   <none>");

      ComparisonResult extra = GoldenComparator.compareText("one\n", "one\ntwo\n");
      assertThat(extra.reported()).containsExactly(Difference.of(2, null, "two"));
      assertThat(extra.report()).contains("line 2 unexpected in actual");
    }

    @Test
    void capsReportedDifferencesButCountsAll() {
      String expected = "a\nb\nc\nd\ne\n";
      String actual = "1\n2\n3\n4\n5\n";
      ComparisonResult result =
          GoldenComparator.compareText(
              expected, actual, TextOptions.defaults().withMaxReportedDifferences(2));

      assertThat(result.totalDifferences()).isEqualTo(5);
      assertThat(result.reported()).hasSize(2);
      assertThat(result.report()).endsWith("... 3 more not shown");
    }

    @Test
    void emptyInputsMatch() {
      assertThat(GoldenComparator.compareText("", "").matches()).isTrue();
      assertThat(GoldenComparator.compareText("", "", TextOptions.strict()).matches()).isTrue();
    }

    @Test
    void rejectsNonPositiveReportCap() {
      assertThatThrownBy(() -> TextOptions.defaults().withMaxReportedDifferences(0))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  class Records {

    private static final int LRECL = 4;

    @Test
    void identicalRecordsMatch() {
      byte[] data = ascii("AAAABBBB");

      ComparisonResult result = GoldenComparator.compareRecords(data, data.clone(), LRECL);

      assertThat(result.matches()).isTrue();
      assertThat(result.expectedCount()).isEqualTo(2);
      assertThat(result.unitName()).isEqualTo("record");
    }

    @Test
    void reportsRecordNumberAndByteOffsetOfFirstMismatch() {
      ComparisonResult result =
          GoldenComparator.compareRecords(ascii("AAAABBBB"), ascii("AAAABBCB"), LRECL);

      assertThat(result.totalDifferences()).isEqualTo(1);
      Difference difference = result.reported().get(0);
      assertThat(difference.position()).isEqualTo(2);
      assertThat(difference.expected()).isEqualTo("BBBB");
      assertThat(difference.actual()).isEqualTo("BBCB");
      assertThat(difference.detail())
          .isEqualTo("first mismatch at offset 2: expected 0x42, actual 0x43");
    }

    @Test
    void rendersControlCharactersAsDots() {
      ComparisonResult result =
          GoldenComparator.compareRecords(
              new byte[] {0x00, 0x01, 'A', 0x7F}, new byte[] {0x00, 0x01, 'B', 0x7F}, LRECL);

      assertThat(result.reported().get(0).expected()).isEqualTo("..A.");
    }

    @Test
    void missingAndExtraRecordsAreDifferences() {
      ComparisonResult shorter =
          GoldenComparator.compareRecords(ascii("AAAABBBB"), ascii("AAAA"), LRECL);
      assertThat(shorter.totalDifferences()).isEqualTo(1);
      assertThat(shorter.reported().get(0)).isEqualTo(Difference.of(2, "BBBB", null));

      ComparisonResult longer =
          GoldenComparator.compareRecords(ascii("AAAA"), ascii("AAAABBBB"), LRECL);
      assertThat(longer.reported().get(0)).isEqualTo(Difference.of(2, null, "BBBB"));
    }

    @Test
    void partialTrailingRecordsNeverMatch() {
      ComparisonResult result =
          GoldenComparator.compareRecords(ascii("AAAABB"), ascii("AAAABB"), LRECL);

      assertThat(result.matches()).isFalse();
      assertThat(result.expectedCount()).isEqualTo(2);
      assertThat(result.reported().get(0).detail())
          .isEqualTo("partial record: expected 2 bytes, actual 2");

      ComparisonResult onlyActualPartial =
          GoldenComparator.compareRecords(ascii("AAAA"), ascii("AAAABB"), LRECL);
      assertThat(onlyActualPartial.reported().get(0).detail()).isEqualTo("partial record: 2 bytes");
    }

    @Test
    void capsReportedDifferences() {
      ComparisonResult result =
          GoldenComparator.compareRecords(ascii("AAAABBBBCCCC"), ascii("111122223333"), LRECL, 1);

      assertThat(result.totalDifferences()).isEqualTo(3);
      assertThat(result.reported()).hasSize(1);
    }

    @Test
    void rejectsInvalidArguments() {
      assertThatThrownBy(() -> GoldenComparator.compareRecords(new byte[0], new byte[0], 0))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> GoldenComparator.compareRecords(new byte[0], new byte[0], 1, 0))
          .isInstanceOf(IllegalArgumentException.class);
    }

    private static byte[] ascii(String s) {
      return s.getBytes(StandardCharsets.US_ASCII);
    }
  }
}
