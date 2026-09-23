package com.cognition.clbs.common.copybook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cognition.clbs.common.copybook.batch.BatchConstants;
import com.cognition.clbs.common.copybook.common.ErrorCodes;
import com.cognition.clbs.common.copybook.common.PortfolioValidation;
import com.cognition.clbs.common.copybook.db2.SqlState;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ConstantsTest {

  @Test
  void errorCodesFromErrhandCopybook() {
    assertThat(ErrorCodes.Standard.fromCode("E002")).isEqualTo(ErrorCodes.Standard.NOT_FOUND);
    assertThat(ErrorCodes.Category.fromCode("VS")).isEqualTo(ErrorCodes.Category.VSAM);
    assertThat(ErrorCodes.VsamStatus.fromCode("23")).isEqualTo(ErrorCodes.VsamStatus.NOT_FOUND);
    assertThat(ErrorCodes.VsamStatus.messageFor("22"))
        .isEqualTo(ErrorCodes.VsamStatus.DUPLICATE_KEY.message());
    assertThat(ErrorCodes.VsamStatus.messageFor("23"))
        .isEqualTo(ErrorCodes.VsamStatus.NOT_FOUND.message());
    assertThat(ErrorCodes.VsamStatus.messageFor("99")).isEqualTo("Unexpected VSAM error");
  }

  @Test
  void portfolioValidationFromPortvalCopybook() {
    assertThat(PortfolioValidation.ID_PREFIX).isEqualTo("PORT");
    assertThat(PortfolioValidation.isAmountInRange(new BigDecimal("9999999999999.99"))).isTrue();
    assertThat(PortfolioValidation.isAmountInRange(new BigDecimal("-9999999999999.99"))).isTrue();
    assertThat(PortfolioValidation.isAmountInRange(new BigDecimal("10000000000000.00"))).isFalse();
    assertThat(PortfolioValidation.Result.fromCode(4))
        .isEqualTo(PortfolioValidation.Result.INVALID_AMOUNT);
    assertThat(PortfolioValidation.Result.INVALID_ID.message())
        .hasSizeLessThanOrEqualTo(PortfolioValidation.MESSAGE_LENGTH);
    assertThatThrownBy(() -> PortfolioValidation.Result.fromCode(9))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void batchConstantsFromBchconCopybook() {
    assertThat(BatchConstants.MAX_PREREQ).isEqualTo(10);
    assertThat(BatchConstants.Status.fromCode("W")).isEqualTo(BatchConstants.Status.WAITING);
    assertThat(BatchConstants.ProcessType.fromCode("RPT"))
        .isEqualTo(BatchConstants.ProcessType.REPORT);
    assertThat(BatchConstants.DependencyType.fromCode("X"))
        .isEqualTo(BatchConstants.DependencyType.EXCLUSIVE);
    assertThat(BatchConstants.ProcessName.fromCode("ENDDAY"))
        .isEqualTo(BatchConstants.ProcessName.END_OF_DAY);
    assertThat(BatchConstants.RecordType.fromCode("H"))
        .isEqualTo(BatchConstants.RecordType.HISTORY);
    for (BatchConstants.Message message : BatchConstants.Message.values()) {
      assertThat(message.text()).hasSizeLessThanOrEqualTo(30);
    }
  }

  @Test
  void sqlStatesFromSqlcaCopybook() {
    assertThat(SqlState.fromCode("23505")).isEqualTo(SqlState.DUPLICATE_KEY);
    for (SqlState state : SqlState.values()) {
      assertThat(state.code()).hasSize(SqlState.LENGTH);
    }
  }
}
