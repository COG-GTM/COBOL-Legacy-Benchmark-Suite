package com.clbs.common.parallelrun;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Dummy-comparison coverage for the parallel-run framework (Phase 0, task 0.6 AC). */
class RecordComparatorTest {

    private final RecordComparator comparator = new RecordComparator();

    private static Map<String, Object> fields(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    @Test
    void identicalOutputsReportNoDifferences() {
        Map<String, Map<String, Object>> cobol = new LinkedHashMap<>();
        cobol.put("PORT0001", fields("status", "A", "totalValue", "12345678.99"));

        Map<String, Map<String, Object>> java = new LinkedHashMap<>();
        java.put("PORT0001", fields("status", "A", "totalValue", "12345678.99"));

        ComparisonResult result = comparator.compare(cobol, java);

        assertThat(result.isIdentical()).isTrue();
        assertThat(result.getMatchedRecords()).isEqualTo(1);
    }

    @Test
    void detectsFieldMissingAndExtraRecords() {
        Map<String, Map<String, Object>> cobol = new LinkedHashMap<>();
        cobol.put("PORT0001", fields("status", "A"));
        cobol.put("PORT0002", fields("status", "C"));

        Map<String, Map<String, Object>> java = new LinkedHashMap<>();
        java.put("PORT0001", fields("status", "S"));
        java.put("PORT0003", fields("status", "A"));

        ComparisonResult result = comparator.compare(cobol, java);

        assertThat(result.isIdentical()).isFalse();
        assertThat(result.getFieldDifferences()).hasSize(1);
        assertThat(result.getMissingInActual()).containsExactly("PORT0002");
        assertThat(result.getExtraInActual()).containsExactly("PORT0003");
        assertThat(result.report()).contains("DIFFERENCES FOUND");
    }
}
