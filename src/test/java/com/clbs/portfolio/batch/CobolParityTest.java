package com.clbs.portfolio.batch;

import com.clbs.portfolio.DatabaseCleaner;
import com.clbs.portfolio.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Replays parity/fixtures through the Java batch jobs in the same order as parity/cobol/run_baseline.sh and
 * compares the output with the GnuCOBOL baseline line by line.
 */
@IntegrationTest
class CobolParityTest {

    private static final Path FIXTURES = Path.of("parity/fixtures");
    private static final Path BASELINE = Path.of("parity/baseline");

    @Autowired
    private PortfolioBatchService batch;

    @Autowired
    private DatabaseCleaner cleaner;

    @BeforeEach
    void setUp() {
        cleaner.clean();
    }

    @Test
    void addUpdateDeleteSequenceMatchesCobol() throws IOException {
        assertMatches(batch.add(read("portadd.dat")).output(), "01-portadd.txt");
        assertMatches(batch.report().output(), "02-portread-after-add.txt");
        assertMatches(batch.update(read("portupdt.dat")).output(), "03-portupdt.txt");
        assertMatches(batch.report().output(), "04-portread-after-update.txt");

        BatchReport delete = batch.delete(read("portdel.dat"));
        assertMatches(delete.output(), "05-portdel.txt");
        List<String> maskedAudit = delete.auditTrail().stream()
                .map(line -> "<TIMESTAMP>" + line.substring(26))
                .toList();
        assertMatches(maskedAudit, "06-portdel-audit.txt");
        assertMatches(batch.report().output(), "07-portread-after-delete.txt");
    }

    private static byte[] read(String fixture) throws IOException {
        return Files.readAllBytes(FIXTURES.resolve(fixture));
    }

    private static void assertMatches(List<String> actual, String baselineFile) throws IOException {
        List<String> expected = Files.readAllLines(BASELINE.resolve(baselineFile), StandardCharsets.ISO_8859_1);
        assertThat(actual).as(baselineFile).containsExactlyElementsOf(expected);
    }
}
