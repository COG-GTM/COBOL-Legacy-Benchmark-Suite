package com.benchmark.portfolio.common.compare;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class FixedWidthRecordReaderTest {

    private static final Path FIXTURES = Path.of("..", "test-fixtures", "data");

    @Test
    void readsPortfolioMasterFixtureWithExactDecimalValues() throws IOException {
        FixedWidthRecordReader reader = new FixedWidthRecordReader(RecordLayout.portfolioMaster());
        List<ComparableRecord> records = reader.readAll(FIXTURES.resolve("portfolio.dat"));

        assertThat(records).hasSize(25);

        ComparableRecord first = records.get(0);
        assertThat(first.value("PORT-ID")).isEqualTo("P0000001");
        assertThat(first.value("PORT-ACCOUNT-NO")).isEqualTo("1469049721");
        assertThat(first.value("PORT-CLIENT-NAME")).isEqualTo("HENRY FISHER");
        assertThat(first.value("PORT-CLIENT-TYPE")).isEqualTo("C");
        assertThat(first.value("PORT-CREATE-DATE")).isEqualTo(new BigDecimal("20230307"));
        assertThat(first.value("PORT-STATUS")).isEqualTo("A");
        assertThat(first.value("PORT-TOTAL-VALUE")).isEqualTo(new BigDecimal("3614309.98"));
        assertThat(first.value("PORT-CASH-BALANCE")).isEqualTo(new BigDecimal("175582.13"));
        assertThat(first.key()).isEqualTo("P0000001");
    }

    @Test
    void readsTransactionFixtureWithExactDecimalValues() throws IOException {
        FixedWidthRecordReader reader = new FixedWidthRecordReader(RecordLayout.transactionRecord());
        List<ComparableRecord> records = reader.readAll(FIXTURES.resolve("transactions.dat"));

        assertThat(records).hasSize(50);

        ComparableRecord first = records.get(0);
        assertThat(first.value("TRN-PORTFOLIO-ID")).isEqualTo("P0000007");
        assertThat(first.value("TRN-SEQUENCE-NO")).isEqualTo("000001");
        assertThat(first.value("TRN-TYPE")).isEqualTo("BU");
        assertThat(first.value("TRN-QUANTITY")).isEqualTo(new BigDecimal("1421.8650"));
        assertThat(first.value("TRN-PRICE")).isEqualTo(new BigDecimal("348.9825"));
        assertThat(first.value("TRN-AMOUNT")).isEqualTo(new BigDecimal("496206.00"));
        assertThat(first.value("TRN-CURRENCY")).isEqualTo("EUR");
        assertThat(first.key()).isEqualTo("P0000007|000001");
    }

    @Test
    void rejectsDataNotAlignedToRecordLength() {
        FixedWidthRecordReader reader = new FixedWidthRecordReader(RecordLayout.portfolioMaster());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> reader.readAll(new byte[147]));
    }

    @Test
    void decodesNegativePackedDecimal() {
        RecordLayout layout = new RecordLayout("NEG", 4,
                List.of(FieldLayout.character("K", 0, 1), FieldLayout.packed("V", 1, 3, 2)),
                List.of("K"));
        byte[] data = {'A', 0x01, 0x23, 0x4D};
        List<ComparableRecord> records = new FixedWidthRecordReader(layout).readAll(data);
        assertThat(records.get(0).value("V")).isEqualTo(new BigDecimal("-12.34"));
    }
}
