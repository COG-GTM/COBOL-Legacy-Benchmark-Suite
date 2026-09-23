package com.cognition.clbs.common.cobol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cognition.clbs.common.copybook.common.CurrencyCode;
import com.cognition.clbs.common.copybook.common.RecordStatus;
import com.cognition.clbs.common.copybook.common.ReturnCode;
import com.cognition.clbs.common.copybook.common.TransactionType;
import org.junit.jupiter.api.Test;

class CobolCodeTest {

  @Test
  void resolvesCommonCopybookLiterals() {
    assertThat(CobolCode.fromCode(RecordStatus.class, "A")).isEqualTo(RecordStatus.ACTIVE);
    assertThat(CobolCode.fromCode(TransactionType.class, "SL")).isEqualTo(TransactionType.SELL);
    assertThat(CobolCode.fromCode(CurrencyCode.class, "JPY")).isEqualTo(CurrencyCode.JPY);
    assertThatThrownBy(() -> CobolCode.fromCode(TransactionType.class, "XX"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("TransactionType");
  }

  @Test
  void returnCodesFollowCommonCopybook() {
    assertThat(ReturnCode.fromValue(0)).isEqualTo(ReturnCode.SUCCESS);
    assertThat(ReturnCode.fromValue(8)).isEqualTo(ReturnCode.ERROR);
    assertThat(ReturnCode.CRITICAL.value()).isEqualTo(16);
    assertThat(ReturnCode.WARNING.max(ReturnCode.SEVERE)).isEqualTo(ReturnCode.SEVERE);
    assertThat(ReturnCode.SEVERE.max(ReturnCode.WARNING)).isEqualTo(ReturnCode.SEVERE);
    assertThatThrownBy(() -> ReturnCode.fromValue(3)).isInstanceOf(IllegalArgumentException.class);
  }
}
