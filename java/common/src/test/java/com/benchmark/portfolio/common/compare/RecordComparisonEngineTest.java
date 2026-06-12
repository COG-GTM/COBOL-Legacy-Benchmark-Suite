package com.benchmark.portfolio.common.compare;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Dummy parallel-run comparison using the golden fixtures: the legacy
 * (expected) stream is the fixed-width .dat file as the COBOL programs would
 * produce it; the actual stream is either the identical data or a copy with
 * seeded mutations.
 */
class RecordComparisonEngineTest {

    private static final Path FIXTURES = Path.of("..", "test-fixtures", "data");

    private final RecordComparisonEngine engine = new RecordComparisonEngine();

    @Test
    void identicalPortfolioStreamsProduceZeroDiffs() throws IOException {
        FixedWidthRecordReader reader = new FixedWidthRecordReader(RecordLayout.portfolioMaster());
        List<ComparableRecord> expected = reader.readAll(FIXTURES.resolve("portfolio.dat"));
        List<ComparableRecord> actual = reader.readAll(FIXTURES.resolve("portfolio.dat"));

        ComparisonReport report = engine.compare(expected, actual);

        assertThat(report.identical()).isTrue();
        assertThat(report.matchedCount()).isEqualTo(25);
        assertThat(report.mismatchedCount()).isZero();
        assertThat(report.missingInActualCount()).isZero();
        assertThat(report.missingInExpectedCount()).isZero();
    }

    @Test
    void identicalTransactionStreamsProduceZeroDiffs() throws IOException {
        FixedWidthRecordReader reader = new FixedWidthRecordReader(RecordLayout.transactionRecord());
        List<ComparableRecord> expected = reader.readAll(FIXTURES.resolve("transactions.dat"));
        List<ComparableRecord> actual = reader.readAll(FIXTURES.resolve("transactions.dat"));

        ComparisonReport report = engine.compare(expected, actual);

        assertThat(report.identical()).isTrue();
        assertThat(report.matchedCount()).isEqualTo(50);
    }

    @Test
    void fixedWidthStreamMatchesCsvParsedEquivalent() throws IOException {
        RecordLayout layout = RecordLayout.portfolioMaster();
        List<ComparableRecord> fromDat =
                new FixedWidthRecordReader(layout).readAll(FIXTURES.resolve("portfolio.dat"));
        List<ComparableRecord> fromCsv =
                new CsvRecordReader(layout).readAll(FIXTURES.resolve("portfolio.csv"));

        ComparisonReport report = engine.compare(fromDat, fromCsv);

        assertThat(report.render()).startsWith("Parallel-run comparison: total=25 matched=25");
        assertThat(report.identical()).isTrue();
    }

    @Test
    void seededMutationsAreDetectedAndReported() throws IOException {
        FixedWidthRecordReader reader = new FixedWidthRecordReader(RecordLayout.portfolioMaster());
        List<ComparableRecord> expected = reader.readAll(FIXTURES.resolve("portfolio.dat"));

        List<ComparableRecord> actual = new ArrayList<>(reader.readAll(FIXTURES.resolve("portfolio.dat")));

        // Mutation 1: one-cent drift in a packed-decimal money field of record P0000003
        actual.set(2, withValue(actual.get(2), "PORT-TOTAL-VALUE",
                ((BigDecimal) actual.get(2).value("PORT-TOTAL-VALUE")).add(new BigDecimal("0.01"))));
        // Mutation 2: changed status field of record P0000005
        actual.set(4, withValue(actual.get(4), "PORT-STATUS", "S"));
        // Mutation 3: record P0000010 missing from the actual stream
        actual.remove(9);

        ComparisonReport report = engine.compare(expected, actual);

        assertThat(report.identical()).isFalse();
        assertThat(report.matchedCount()).isEqualTo(22);
        assertThat(report.mismatchedCount()).isEqualTo(2);
        assertThat(report.missingInActualCount()).isEqualTo(1);
        assertThat(report.missingInExpectedCount()).isZero();

        RecordDiff moneyDiff = report.diffsWithStatus(RecordDiff.Status.MISMATCHED).get(0);
        assertThat(moneyDiff.key()).isEqualTo("P0000003");
        assertThat(moneyDiff.fieldDiffs()).hasSize(1);
        FieldDiff fieldDiff = moneyDiff.fieldDiffs().get(0);
        assertThat(fieldDiff.fieldName()).isEqualTo("PORT-TOTAL-VALUE");
        assertThat(((BigDecimal) fieldDiff.actualValue())
                .subtract((BigDecimal) fieldDiff.expectedValue()))
                .isEqualByComparingTo("0.01");

        RecordDiff statusDiff = report.diffsWithStatus(RecordDiff.Status.MISMATCHED).get(1);
        assertThat(statusDiff.key()).isEqualTo("P0000005");
        assertThat(statusDiff.fieldDiffs().get(0).fieldName()).isEqualTo("PORT-STATUS");

        assertThat(report.diffsWithStatus(RecordDiff.Status.MISSING_IN_ACTUAL).get(0).key())
                .isEqualTo("P0000010");

        assertThat(report.render())
                .contains("mismatched=2")
                .contains("PORT-TOTAL-VALUE")
                .contains("[MISSING_IN_ACTUAL] key=P0000010");
    }

    @Test
    void numericComparisonIsScaleInsensitiveButValueExact() {
        ComparableRecord expected = record("K1", new BigDecimal("100.10"));
        ComparableRecord sameValue = record("K1", new BigDecimal("100.1000"));
        ComparableRecord tinyDrift = record("K1", new BigDecimal("100.1001"));

        assertThat(engine.compare(List.of(expected), List.of(sameValue)).identical()).isTrue();
        assertThat(engine.compare(List.of(expected), List.of(tinyDrift)).mismatchedCount()).isEqualTo(1);
    }

    @Test
    void ignoredFieldsAreExcludedFromComparison() {
        RecordComparisonEngine ignoringEngine = new RecordComparisonEngine(java.util.Set.of("AMOUNT"));
        ComparableRecord expected = record("K1", new BigDecimal("1.00"));
        ComparableRecord actual = record("K1", new BigDecimal("2.00"));

        assertThat(ignoringEngine.compare(List.of(expected), List.of(actual)).identical()).isTrue();
    }

    @Test
    void extraRecordInActualStreamIsReported() {
        ComparableRecord shared = record("K1", new BigDecimal("1.00"));
        ComparableRecord extra = record("K2", new BigDecimal("2.00"));

        ComparisonReport report = engine.compare(List.of(shared), List.of(shared, extra));

        assertThat(report.missingInExpectedCount()).isEqualTo(1);
        assertThat(report.diffsWithStatus(RecordDiff.Status.MISSING_IN_EXPECTED).get(0).key()).isEqualTo("K2");
    }

    private static ComparableRecord record(String key, BigDecimal amount) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("KEY", key);
        values.put("AMOUNT", amount);
        return new ComparableRecord(values, List.of("KEY"));
    }

    private static ComparableRecord withValue(ComparableRecord record, String fieldName, Object value) {
        Map<String, Object> values = new LinkedHashMap<>(record.values());
        values.put(fieldName, value);
        return new ComparableRecord(values, record.keyFields());
    }
}
