package com.benchmark.portfolio.common.compare;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class YamlLayoutLoaderTest {

    @Test
    void loadsPortfolioLayoutFromYamlAndReadsFixture() throws IOException {
        RecordLayout layout = new YamlLayoutLoader()
                .load(getClass().getResourceAsStream("/layouts/portfolio-master.yaml"));

        assertThat(layout).isEqualTo(RecordLayout.portfolioMaster());

        List<ComparableRecord> records = new FixedWidthRecordReader(layout)
                .readAll(Path.of("..", "test-fixtures", "data", "portfolio.dat"));
        assertThat(records).hasSize(25);
        assertThat(records.get(0).value("PORT-ID")).isEqualTo("P0000001");
    }

    @Test
    void rejectsLayoutMissingRequiredKeys() {
        String yaml = "name: BROKEN\nrecordLength: 10\n";
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new YamlLayoutLoader().load(
                        new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8))));
    }
}
