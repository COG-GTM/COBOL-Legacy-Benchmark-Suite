package com.cognition.clbs.common.copybook;

import static org.assertj.core.api.Assertions.assertThat;

import com.cognition.clbs.common.cobol.CobolCharset;
import com.cognition.clbs.common.cobol.CopybookRecord;
import com.cognition.clbs.common.copybook.common.AuditFields;
import com.cognition.clbs.common.copybook.common.AuditRecord;
import com.cognition.clbs.common.copybook.common.CommonDateTime;
import com.cognition.clbs.common.copybook.common.ErrorHandlingArea;
import com.cognition.clbs.common.copybook.common.ErrorMessage;
import com.cognition.clbs.common.copybook.common.HistoryRecord;
import com.cognition.clbs.common.copybook.common.PortfolioRecord;
import com.cognition.clbs.common.copybook.common.PositionRecord;
import com.cognition.clbs.common.copybook.common.ReturnCode;
import com.cognition.clbs.common.copybook.common.ReturnCodeArea;
import com.cognition.clbs.common.copybook.common.ReturnHandling;
import com.cognition.clbs.common.copybook.common.TransactionRecord;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class CommonCopybookRoundTripTest {

  private static <T extends CopybookRecord> void assertRoundTrip(
      T record, int expectedLength, Function<byte[], T> parse) {
    byte[] ascii = record.toBytes();
    assertThat(ascii).hasSize(expectedLength);
    assertThat(record.recordLength()).isEqualTo(expectedLength);
    assertThat(parse.apply(ascii)).isEqualTo(record);
  }

  @Test
  void portfolioRecordMatchesPortflioCopybook() {
    PortfolioRecord record = SampleRecords.portfolio();
    assertRoundTrip(record, PortfolioRecord.LENGTH, PortfolioRecord::parse);
    assertThat(PortfolioRecord.LENGTH).isEqualTo(148);

    byte[] bytes = record.toBytes();
    assertThat(new String(bytes, 0, PortfolioRecord.KEY_LENGTH, StandardCharsets.US_ASCII))
        .isEqualTo("PORT0001ACC0000001");
    assertThat(new String(bytes, 49, 8, StandardCharsets.US_ASCII)).isEqualTo("20240101");
    assertThat(PortfolioRecord.parse(record.toBytes(CobolCharset.EBCDIC), CobolCharset.EBCDIC))
        .isEqualTo(record);
  }

  @Test
  void positionRecordMatchesPosrecCopybook() {
    assertRoundTrip(SampleRecords.position(), 138, PositionRecord::parse);
    assertThat(PositionRecord.KEY_LENGTH).isEqualTo(26);
  }

  @Test
  void transactionRecordMatchesTrnrecCopybook() {
    assertRoundTrip(SampleRecords.transaction(), 152, TransactionRecord::parse);
    assertThat(TransactionRecord.KEY_LENGTH).isEqualTo(28);
  }

  @Test
  void historyRecordMatchesHistrecCopybook() {
    assertRoundTrip(SampleRecords.history(), 917, HistoryRecord::parse);
    assertThat(HistoryRecord.KEY_LENGTH).isEqualTo(26);
  }

  @Test
  void auditRecordMatchesAuditlogCopybook() {
    assertRoundTrip(SampleRecords.audit(), 392, AuditRecord::parse);
  }

  @Test
  void commonWorkingStorageAreas() {
    assertRoundTrip(
        new CommonDateTime("2024", "03", "15", "14", "30", "45", "12"), 16, CommonDateTime::parse);
    assertRoundTrip(
        new ErrorHandlingArea("E001", "PORTUPDT", "VALIDATE", "Invalid data"),
        100,
        ErrorHandlingArea::parse);
    assertRoundTrip(
        new AuditFields(SampleRecords.TIMESTAMP, "OPERATOR", "T001", "PORTUPDT"),
        50,
        AuditFields::parse);
  }

  @Test
  void returnCodeAreaMatchesRtncodeCopybook() {
    ReturnCodeArea area =
        new ReturnCodeArea(
            "S",
            "PORTUPDT",
            4,
            8,
            0,
            "W",
            "Warning issued",
            0,
            SampleRecords.TIMESTAMP,
            SampleRecords.TIMESTAMP,
            12,
            8,
            0,
            8,
            8,
            "W");
    assertRoundTrip(area, 165, ReturnCodeArea::parse);
    assertThat(ReturnCodeArea.RequestType.fromCode("S"))
        .isEqualTo(ReturnCodeArea.RequestType.SET_CODE);
    assertThat(ReturnCodeArea.Status.fromCode("W")).isEqualTo(ReturnCodeArea.Status.WARNING);
  }

  @Test
  void returnHandlingMatchesRethndCopybook() {
    ReturnHandling handling =
        new ReturnHandling(
            8,
            100,
            "PORTFOLI",
            "UPDATE",
            "PORTUPDT",
            "2000-PRC",
            "9000-ERR",
            "D",
            "E005",
            "SQLCODE -911 deadlock",
            "-911",
            "Deadlock or timeout",
            "R",
            1,
            ReturnHandling.DEFAULT_MAX_RETRIES);
    assertRoundTrip(handling, 218, ReturnHandling::parse);
    assertThat(handling.returnCodeValue()).isEqualTo(ReturnCode.ERROR);
    assertThat(ReturnHandling.ErrorType.fromCode("D")).isEqualTo(ReturnHandling.ErrorType.DATABASE);
    assertThat(ReturnHandling.Action.fromCode("R")).isEqualTo(ReturnHandling.Action.RETRY);
  }

  @Test
  void errorMessageMatchesErrhandCopybook() {
    ErrorMessage message =
        new ErrorMessage(
            "20240315", "143045", "PORTUPDT", "VS", "E004", 8, "File error", "status 23");
    assertRoundTrip(message, 370, ErrorMessage::parse);
  }
}
