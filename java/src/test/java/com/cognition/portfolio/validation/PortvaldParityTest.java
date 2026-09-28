package com.cognition.portfolio.validation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Behavioural parity between the COBOL PORTVALD and {@link PortfolioValidator}.
 *
 * <p>{@link Recorded} always runs and compares Java against the COBOL outputs recorded in
 * portvald-cobol-golden.tsv. {@link Live} runs only when {@code -Dparity.cobol.driver} and
 * {@code -Dparity.cobol.libpath} point at a GnuCOBOL build (java/run-parity.sh does this): it
 * executes the compiled COBOL for every case, compares Java against those fresh outputs, and
 * fails if the recorded file no longer matches the live COBOL.
 */
class PortvaldParityTest {

    static final String DRIVER_PROPERTY = "parity.cobol.driver";
    static final String LIBPATH_PROPERTY = "parity.cobol.libpath";

    private static final PortfolioValidator VALIDATOR = new PortfolioValidator();
    private static final List<ParityCase> CASES = ParityData.loadCases();

    static List<ParityCase> cases() {
        return CASES;
    }

    private static void assertJavaMatchesCobol(ParityCase c, CobolResult cobol) {
        ValidationResult java = VALIDATOR.validate(c.validateType(), c.inputValue());
        BigDecimal javaMove = DisplayNumericMove.alphanumericToSignedDisplay(
                AlphanumericField.of(c.inputValue(), PortvalConstants.FIELD_LENGTH),
                PortvalConstants.TEMP_NUM_INTEGER_DIGITS, PortvalConstants.TEMP_NUM_SCALE);
        assertAll(c.id(),
                () -> assertEquals(cobol.returnCode(), java.returnCode(), "LS-RETURN-CODE"),
                () -> assertEquals(cobol.errorMessage(), java.errorMessage(), "LS-ERROR-MSG (all 50 bytes)"),
                () -> assertEquals(0, cobol.callReturnCode(), "PORTVALD must leave RETURN-CODE at 0"),
                () -> assertEquals(0, cobol.moveProbeValue().compareTo(javaMove),
                        "VAL-TEMP-NUM after MOVE: COBOL " + cobol.moveProbe() + ", Java " + javaMove.toPlainString()));
    }

    @Nested
    @DisplayName("Java vs recorded COBOL outputs (portvald-cobol-golden.tsv)")
    class Recorded {

        private final Map<String, CobolResult> golden = ParityData.loadGolden();

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.cognition.portfolio.validation.PortvaldParityTest#cases")
        void javaMatchesRecordedCobol(ParityCase c) {
            CobolResult cobol = golden.get(c.id());
            assertNotNull(cobol, "no recorded COBOL result for " + c.id() + "; run java/run-parity.sh --update-golden");
            assertJavaMatchesCobol(c, cobol);
        }

        @Test
        void recordedResultsCoverExactlyTheCaseTable() {
            assertEquals(CASES.stream().map(ParityCase::id).collect(Collectors.toList()),
                    new ArrayList<>(golden.keySet()));
        }

        @Test
        void caseTableExercisesEveryDispatchBranchAndReturnCode() {
            Set<Character> types = CASES.stream().map(ParityCase::validateType).collect(Collectors.toSet());
            assertTrue(types.containsAll(Set.of('I', 'A', 'T', 'M')), "every 88-level type");
            assertTrue(types.stream().anyMatch(t -> "IATM".indexOf(t) < 0), "WHEN OTHER");

            Set<String> reached = golden.values().stream()
                    .map(r -> r.returnCode() + ":" + r.errorMessage().stripTrailing())
                    .collect(Collectors.toCollection(TreeSet::new));
            assertEquals(new TreeSet<>(Set.of(
                    "0:",
                    "1:Invalid Portfolio ID format",
                    "1:Invalid validation type",
                    "2:Invalid Account Number format",
                    "3:Invalid Investment Type")), reached,
                    "distinct (return code, message) outcomes produced by the COBOL; rc 4 is unreachable, see migration doc");
        }
    }

    @Nested
    @DisplayName("Java vs live GnuCOBOL execution of PORTVALD")
    @EnabledIfSystemProperty(named = DRIVER_PROPERTY, matches = ".+")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class Live {

        private final Path workDir = Path.of("target", "parity");
        private Map<String, CobolResult> live;

        @BeforeAll
        void runCobol() throws Exception {
            Path driver = Path.of(System.getProperty(DRIVER_PROPERTY));
            Path libPath = Path.of(System.getProperty(LIBPATH_PROPERTY, driver.getParent().toString()));
            List<CobolResult> results = CobolOracle.run(driver, libPath, workDir, CASES);
            ParityData.writeGolden(workDir.resolve("portvald-cobol-live.tsv"), results);
            live = results.stream().collect(Collectors.toMap(CobolResult::caseId, r -> r,
                    (a, b) -> { throw new IllegalStateException("duplicate " + a.caseId()); },
                    LinkedHashMap::new));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.cognition.portfolio.validation.PortvaldParityTest#cases")
        void javaMatchesLiveCobol(ParityCase c) {
            assertJavaMatchesCobol(c, live.get(c.id()));
        }

        @Test
        void recordedResultsMatchLiveCobol() {
            assertEquals(new ArrayList<>(ParityData.loadGolden().values()), new ArrayList<>(live.values()),
                    "portvald-cobol-golden.tsv is stale; diff it against target/parity/portvald-cobol-live.tsv");
        }
    }
}
