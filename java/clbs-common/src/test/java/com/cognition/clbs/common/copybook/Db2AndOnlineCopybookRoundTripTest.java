package com.cognition.clbs.common.copybook;

import static org.assertj.core.api.Assertions.assertThat;

import com.cognition.clbs.common.copybook.db2.Db2ErrorHandling;
import com.cognition.clbs.common.copybook.db2.ErrorLogRecord;
import com.cognition.clbs.common.copybook.db2.PositionHistoryRecord;
import com.cognition.clbs.common.copybook.online.Db2RequestArea;
import com.cognition.clbs.common.copybook.online.InquiryCommArea;
import com.cognition.clbs.common.copybook.online.OnlineErrorArea;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class Db2AndOnlineCopybookRoundTripTest {

  @Test
  void positionHistoryHostVariablesMatchDbtblsCopybook() {
    PositionHistoryRecord record = SampleRecords.positionHistory();
    byte[] bytes = record.toBytes();
    assertThat(bytes).hasSize(166);
    assertThat(PositionHistoryRecord.parse(bytes)).isEqualTo(record);
  }

  @Test
  void errorLogHostVariablesMatchDbtblsCopybook() {
    ErrorLogRecord record = SampleRecords.errorLog();
    byte[] bytes = record.toBytes();
    assertThat(bytes).hasSize(771);
    assertThat(ErrorLogRecord.parse(bytes)).isEqualTo(record);
    assertThat(ErrorLogRecord.ErrorType.fromCode("D")).isEqualTo(ErrorLogRecord.ErrorType.DATA);
    assertThat(ErrorLogRecord.Severity.fromValue(4)).isEqualTo(ErrorLogRecord.Severity.SEVERE);
  }

  @Test
  void db2ErrorHandlingKeepsLiteralFillerText() {
    Db2ErrorHandling area =
        new Db2ErrorHandling(
            "-911",
            "40001",
            "Deadlock",
            "00000",
            1,
            Db2ErrorHandling.DEFAULT_MAX_RETRIES,
            Db2ErrorHandling.DEFAULT_RETRY_WAIT);
    byte[] bytes = area.toBytes();
    assertThat(bytes).hasSize(118);
    String message =
        new String(bytes, 0, Db2ErrorHandling.MESSAGE_LENGTH, StandardCharsets.US_ASCII);
    assertThat(message).startsWith("SQLCODE: -911   STATE:  40001 ERROR: Deadlock");
    assertThat(area.formatMessage()).isEqualTo(message).hasSize(107);
    assertThat(Db2ErrorHandling.parse(bytes)).isEqualTo(area);
  }

  @Test
  void db2RequestAreaMatchesDb2reqCopybook() {
    Db2RequestArea area = new Db2RequestArea("C", 0, "TOKEN-0000000001", -911, "");
    byte[] bytes = area.toBytes();
    assertThat(bytes).hasSize(105);
    assertThat(Db2RequestArea.parse(bytes)).isEqualTo(area);
    assertThat(Db2RequestArea.RequestType.fromCode("C"))
        .isEqualTo(Db2RequestArea.RequestType.CONNECT);
  }

  @Test
  void onlineErrorAreaMatchesErrhndCopybook() {
    OnlineErrorArea area =
        new OnlineErrorArea(
            "INQPORT",
            "2000-READ-PORTFOLIO",
            100,
            13,
            80,
            "F",
            "Record not found",
            "R",
            "TRACE-00000000001",
            SampleRecords.TIMESTAMP);
    byte[] bytes = area.toBytes();
    assertThat(bytes).hasSize(174);
    OnlineErrorArea parsed = OnlineErrorArea.parse(bytes);
    assertThat(parsed.traceId()).isEqualTo("TRACE-0000000000"); // PIC X(16) truncates on MOVE
    assertThat(parsed.severity()).isEqualTo("F");
    assertThat(OnlineErrorArea.Severity.fromCode("F")).isEqualTo(OnlineErrorArea.Severity.FATAL);
    assertThat(OnlineErrorArea.Action.fromCode("A")).isEqualTo(OnlineErrorArea.Action.ABEND);
  }

  @Test
  void inquiryCommAreaMatchesInqcomCopybook() {
    InquiryCommArea area = new InquiryCommArea("INQP", "ACC0000001", 0, "");
    byte[] bytes = area.toBytes();
    assertThat(bytes).hasSize(98);
    assertThat(InquiryCommArea.parse(bytes)).isEqualTo(area);
    assertThat(InquiryCommArea.Function.fromCode("INQH"))
        .isEqualTo(InquiryCommArea.Function.HISTORY);
  }
}
