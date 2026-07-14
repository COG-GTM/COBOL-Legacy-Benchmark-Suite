package com.cog.gtm.clbs.migration.golden;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cog.gtm.clbs.migration.service.validation.PortfolioValidationService;
import com.cog.gtm.clbs.migration.service.validation.ValidationResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PortfolioValidationGoldenTest {

    private static final String INPUTS_PATH = "fixtures/portfolio-validation/inputs.csv";
    private static final String EXPECTED_PATH = "fixtures/portfolio-validation/expected-outputs.csv";

    @Autowired
    private PortfolioValidationService validationService;

    @Test
    void goldenMasterMatchesExpectedOutputs() throws IOException {
        List<GoldenMasterCase> cases = GoldenMasterFixture.load(INPUTS_PATH, EXPECTED_PATH);
        List<String> deviations = new ArrayList<>();

        for (GoldenMasterCase testCase : cases) {
            ValidationResult actual = validationService.validate(testCase.validationType(), testCase.inputValue());
            if (!actual.matches(testCase.expectedResult())) {
                deviations.add(String.format(
                        "type=%s, input=%s -> expected [rc=%d, msg='%s'], actual [rc=%d, msg='%s']",
                        testCase.validationType(),
                        testCase.inputValue(),
                        testCase.expectedResult().returnCode(),
                        testCase.expectedResult().errorMessage(),
                        actual.returnCode(),
                        actual.errorMessage()));
            }
        }

        assertTrue(deviations.isEmpty(),
                "Golden master deviations detected:" + System.lineSeparator() + String.join(System.lineSeparator(), deviations));
    }
}
