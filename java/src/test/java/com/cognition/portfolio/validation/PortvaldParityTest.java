package com.cognition.portfolio.validation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives the Java port with the same case table that
 * {@code parity/cobol/PVDRIVER.cbl} feeds to the compiled PORTVALD module,
 * and asserts that every observable output is identical.
 *
 * <p>The COBOL side of the comparison is the file
 * {@code parity/expected/portvald-cobol-output.psv}, which is produced by
 * running the GnuCOBOL build. {@code parity/run-parity.sh} regenerates it and
 * fails if it differs from the committed copy, so a stale file cannot make
 * this test pass.</p>
 */
class PortvaldParityTest {

    private record Case(String id, char validateType, String inputValue, String description) {
    }

    private final PortfolioValidator validator = new PortfolioValidator();

    private static List<Case> cases() {
        List<Case> cases = new ArrayList<>();
        for (String line : ParityFiles.readLines(ParityFiles.CASES)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] fields = line.split("\\|", -1);
            assertEquals(4, fields.length, "malformed case line: " + line);
            String validateType = fields[1];
            assertEquals(1, validateType.length(), "validate_type must be one byte: " + line);
            cases.add(new Case(fields[0], validateType.charAt(0), fields[2], fields[3]));
        }
        return cases;
    }

    private static Map<String, String> cobolOutput() {
        Map<String, String> rows = new LinkedHashMap<>();
        for (String line : ParityFiles.readLines(ParityFiles.COBOL_OUTPUT)) {
            if (line.isBlank() || line.startsWith("case_id|")) {
                continue;
            }
            rows.put(line.substring(0, line.indexOf('|')), line);
        }
        return rows;
    }

    /** Formats one result the way PVDRIVER writes it, so the two files compare byte for byte. */
    private String render(String caseId, ValidationResult result, BigDecimal amountMove) {
        return caseId
                + "|" + formatSigned(BigDecimal.valueOf(result.returnCode()), 4, 0)
                + "|" + formatSigned(amountMove, 13, 2)
                + "|[" + result.errorMessage() + "]";
    }

    /** PIC -9(n) and PIC -9(n).99 editing: leading sign position, zero filled. */
    private static String formatSigned(BigDecimal value, int integerDigits, int fractionDigits) {
        BigDecimal absolute = value.abs();
        String digits = absolute.unscaledValue().toString();
        String padded = "0".repeat(Math.max(0, integerDigits + fractionDigits - digits.length())) + digits;
        String integerPart = padded.substring(0, integerDigits);
        String fractionPart = padded.substring(integerDigits);
        String sign = value.signum() < 0 ? "-" : " ";
        return fractionDigits == 0 ? sign + integerPart : sign + integerPart + "." + fractionPart;
    }

    @TestFactory
    List<DynamicTest> javaMatchesCompiledCobolForEveryCase() {
        Map<String, String> cobol = cobolOutput();
        return cases().stream()
                .map(testCase -> DynamicTest.dynamicTest(
                        testCase.id() + " " + testCase.description(),
                        () -> {
                            String expected = cobol.get(testCase.id());
                            assertTrue(expected != null,
                                    "no COBOL output recorded for case " + testCase.id());
                            ValidationResult result =
                                    validator.validate(testCase.validateType(), testCase.inputValue());
                            String actual = render(
                                    testCase.id(), result, validator.amountAsMoved(testCase.inputValue()));
                            assertEquals(expected, actual,
                                    "Java and COBOL disagree for case " + testCase.id()
                                            + " (" + testCase.description() + ")");
                        }))
                .collect(Collectors.toList());
    }

    @Test
    void everyCobolRowHasACase() {
        Set<String> caseIds = cases().stream().map(Case::id).collect(Collectors.toCollection(LinkedHashSet::new));
        assertEquals(caseIds, cobolOutput().keySet(),
                "the case table and the recorded COBOL outputs must line up exactly");
    }

    @Test
    void caseTableCoversEveryDispatchBranchAndEveryReachableReturnCode() {
        List<Case> cases = cases();
        assertTrue(cases.size() >= 70, "expected a substantial case table, found " + cases.size());

        Set<Character> dispatched = cases.stream()
                .map(Case::validateType)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        assertTrue(dispatched.containsAll(Set.of('I', 'A', 'T', 'M')),
                "every EVALUATE branch must be exercised");
        assertTrue(dispatched.stream().anyMatch(c -> ValidationType.fromCode(c).isEmpty()),
                "the WHEN OTHER branch must be exercised");

        Set<Integer> returnCodes = cases.stream()
                .map(c -> validator.validate(c.validateType(), c.inputValue()).returnCode())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        assertEquals(Set.of(PortValConstants.VAL_SUCCESS,
                        PortValConstants.VAL_INVALID_ID,
                        PortValConstants.VAL_INVALID_ACCT,
                        PortValConstants.VAL_INVALID_TYPE),
                returnCodes,
                "VAL-INVALID-AMT (4) is unreachable in PORTVALD; if this set changes, the"
                        + " migration document's known gaps section is out of date");
    }
}
