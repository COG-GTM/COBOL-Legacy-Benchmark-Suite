package com.clbs.portfolio.testdata;

import static org.assertj.core.api.Assertions.assertThat;

import com.clbs.common.cobol.CobolField;
import com.clbs.common.parallelrun.ComparisonResult;
import com.clbs.common.parallelrun.RecordComparator;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Verifies the committed golden fixtures (Phase 0, task 0.7) and exercises the
 * parallel-run comparison framework end-to-end against them (task 0.6).
 */
class GoldenFixtureTest {

    private static final int PORTFOLIO_COUNT = 5;
    private static final int TRANSACTION_COUNT = 5;

    private static String readResource(String path) throws IOException {
        try (InputStream in = GoldenFixtureTest.class.getResourceAsStream(path)) {
            assertThat(in).as("fixture resource %s must be checked in", path).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void committedFixturesMatchGenerator() throws IOException {
        assertThat(readResource("/fixtures/portfolio.dat"))
                .isEqualTo(GoldenFixtures.portfolioFile(PORTFOLIO_COUNT));
        assertThat(readResource("/fixtures/transaction.dat"))
                .isEqualTo(GoldenFixtures.transactionFile(TRANSACTION_COUNT));
    }

    @Test
    void fixturesHaveCopybookRecordLengths() throws IOException {
        for (String line : readResource("/fixtures/portfolio.dat").split("\n")) {
            assertThat(line).hasSize(GoldenFixtures.PORTFOLIO_RECORD_LENGTH);
        }
        for (String line : readResource("/fixtures/transaction.dat").split("\n")) {
            assertThat(line).hasSize(GoldenFixtures.TRANSACTION_RECORD_LENGTH);
        }
    }

    @Test
    void parallelRunComparisonOfFixturesIsIdentical() throws IOException {
        // "Legacy" side: parse the fixed-width golden file (as a COBOL extract would be read).
        Map<String, Map<String, Object>> expected = new LinkedHashMap<>();
        for (String line : readResource("/fixtures/portfolio.dat").split("\n")) {
            String portId = CobolField.parseAlphanumeric(line.substring(0, 8));
            String accountNo = CobolField.parseAlphanumeric(line.substring(8, 18));
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("clientName", CobolField.parseAlphanumeric(line.substring(18, 48)));
            // offsets: clientType[48,49) createDate[49,57) lastMaint[57,65) status[65,66) totalValue[66,81)
            fields.put("status", line.substring(65, 66));
            fields.put("totalValue", CobolField.parseNumeric(line.substring(66, 81), 2).toPlainString());
            expected.put(portId + "|" + accountNo, fields);
        }

        // "Migrated" side: build the same view directly from the Java generator.
        Map<String, Map<String, Object>> actual = new LinkedHashMap<>();
        for (GoldenFixtures.PortfolioFixture row : GoldenFixtures.portfolios(PORTFOLIO_COUNT)) {
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("clientName", row.clientName());
            fields.put("status", row.status());
            fields.put("totalValue", row.totalValue().toPlainString());
            actual.put(row.portId() + "|" + row.accountNo(), fields);
        }

        ComparisonResult result = new RecordComparator().compare(expected, actual);
        assertThat(result.isIdentical()).as(result.report()).isTrue();
        assertThat(result.getMatchedRecords()).isEqualTo(PORTFOLIO_COUNT);
    }
}
