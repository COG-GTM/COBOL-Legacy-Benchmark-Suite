package com.clbs.testdata;

import static org.assertj.core.api.Assertions.assertThat;

import com.clbs.common.ProgramResult;
import com.clbs.testdata.TestValidationProgram.TestCase;
import java.util.List;
import org.junit.jupiter.api.Test;

class TestValidationProgramTest {

    private final TestValidationProgram program = new TestValidationProgram();

    @Test
    void countsPassedAndFailedCases() {
        ProgramResult result = program.run(List.of(
                new TestCase("TC001", TestValidationProgram.FUNCTIONAL, "buy", "100", "100"),
                new TestCase("TC002", TestValidationProgram.ERROR, "sell", "100", "090")));

        assertThat(result.counter("total")).isEqualTo(2);
        assertThat(result.counter("passed")).isEqualTo(1);
        assertThat(result.counter("failed")).isEqualTo(1);
        assertThat(result.getReturnCode()).isEqualTo(4);
        assertThat(result.getDisplay()).anyMatch(line -> line.contains("SUCCESS:"));
    }

    @Test
    void unknownTestTypeStopsTheRun() {
        ProgramResult result = program.run(List.of(
                new TestCase("TC001", "NOPE", "buy", "1", "1")));

        assertThat(result.getReturnCode()).isEqualTo(12);
        assertThat(result.getDisplay()).contains(TestValidationProgram.ERR_INVALID_TYPE);
        assertThat(result.counter("total")).isZero();
    }

    @Test
    void successRateIsGuardedAndScaledToTwoDecimals() {
        assertThat(TestValidationProgram.successRate(0, 0)).isEqualByComparingTo("0.00");
        assertThat(TestValidationProgram.successRate(1, 3)).isEqualByComparingTo("33.33");
        assertThat(TestValidationProgram.successRate(3, 3)).isEqualByComparingTo("100.00");
    }
}
