package com.cognition.clbs.reporting.format;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class NumericPictureTest {

  @ParameterizedTest(name = "{0} <- {1} = ''{2}''")
  @CsvSource(
      delimiter = '|',
      value = {
        "ZZZ,ZZZ,ZZ9.99 | 0            | '          0.00'",
        "ZZZ,ZZZ,ZZ9.99 | 1234.5       | '      1,234.50'",
        "ZZZ,ZZZ,ZZ9.99 | 1234567.891  | '  1,234,567.89'",
        "ZZZ,ZZZ,ZZ9.99 | -42.999      | '         42.99'",
        "ZZZ,ZZZ,ZZ9.99 | 12345678901  | '345,678,901.00'",
        "$$$$,$$$,$$9.99 | 0           | '          $0.00'",
        "$$$$,$$$,$$9.99 | 12.5        | '         $12.50'",
        "$$$$,$$$,$$9.99 | 1234567.89  | '  $1,234,567.89'",
        "$$$$,$$$,$$9.99 | 123456789.5 | '$123,456,789.50'",
        "+ZZ9.99 | 0       | '+  0.00'",
        "+ZZ9.99 | 12.345  | '+ 12.34'",
        "+ZZ9.99 | -3.5    | '-  3.50'",
        "+ZZ9.99 | 1234.56 | '+234.56'",
        "ZZZ,ZZZ,ZZ9 | 0       | '          0'",
        "ZZZ,ZZZ,ZZ9 | 9876543 | '  9,876,543'",
        "ZZ,ZZ9.999 | 0.1234 | '     0.123'",
        "ZZ,ZZ9.999 | 12345.6 | '12,345.600'",
        "ZZZ,ZZ9 | 42 | '     42'",
        "ZZ9.99 | 100 | '100.00'",
        "ZZ9.99 | 66.666 | ' 66.66'",
        "-ZZ9.99 | 5 | '   5.00'",
        "-ZZ9.99 | -5 | '-  5.00'",
      })
  void editsLikeCobolMove(String picture, String value, String expected) {
    NumericPicture pic = NumericPicture.of(picture);
    String formatted = pic.format(new BigDecimal(value));
    assertThat(formatted).isEqualTo(expected);
    assertThat(formatted).hasSize(pic.length());
  }

  @Test
  void floatingDollarWithZeroAndAllFloatingIntegerPositions() {
    assertThat(NumericPicture.of("$$$.99").format(BigDecimal.ZERO)).isEqualTo("  $.00");
    assertThat(NumericPicture.of("$$$.99").format(new BigDecimal("7"))).isEqualTo(" $7.00");
  }

  @Test
  void longOverloadAndNullValue() {
    NumericPicture pic = NumericPicture.of("ZZ9");
    assertThat(pic.format(7L)).isEqualTo("  7");
    assertThat(pic.format((BigDecimal) null)).isEqualTo("  0");
    assertThat(pic.picture()).isEqualTo("ZZ9");
    assertThat(pic).hasToString("PIC ZZ9");
  }

  @Test
  void rejectsUnsupportedPictures() {
    assertThatThrownBy(() -> NumericPicture.of("")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> NumericPicture.of("ZZ9.ZZ"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> NumericPicture.of("ZZ9X"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
