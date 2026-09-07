package com.clbs.api;

import com.clbs.common.ProgramResult;
import com.clbs.testdata.TestDataGenerator;
import com.clbs.testdata.TestValidationProgram;
import com.clbs.utility.DataValidationUtility;
import com.clbs.utility.FileMaintenanceUtility;
import com.clbs.utility.SystemMonitorUtility;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** HTTP entry points for the utility (UTL*) and test-support (TST*) programs. */
@RestController
@RequestMapping("/api/utility")
public class UtilityController {

    private final FileMaintenanceUtility maintenance;
    private final SystemMonitorUtility monitor;
    private final DataValidationUtility validation;
    private final TestDataGenerator generator;
    private final TestValidationProgram testValidation;

    public UtilityController(FileMaintenanceUtility maintenance, SystemMonitorUtility monitor,
            DataValidationUtility validation, TestDataGenerator generator,
            TestValidationProgram testValidation) {
        this.maintenance = maintenance;
        this.monitor = monitor;
        this.validation = validation;
        this.generator = generator;
        this.testValidation = testValidation;
    }

    /** UTLMNT00. */
    @PostMapping("/maintenance")
    public ProgramResult maintenance(
            @RequestBody List<FileMaintenanceUtility.ControlRecord> controls) {
        return maintenance.run(controls);
    }

    /** UTLMON00 (one cycle). */
    @PostMapping("/monitor")
    public SystemMonitorUtility.MonitorCycle monitor(@RequestBody MonitorPayload payload) {
        return monitor.cycle(payload.configs(), payload.metrics());
    }

    /** Request body for one UTLMON00 cycle. */
    public record MonitorPayload(List<SystemMonitorUtility.MonitorConfig> configs,
            List<SystemMonitorUtility.Metric> metrics) {
    }

    /** UTLVAL00. */
    @PostMapping("/validate")
    public ProgramResult validate(
            @RequestBody List<DataValidationUtility.ValidationRequest> requests) {
        return validation.run(requests);
    }

    /** TSTGEN00. */
    @PostMapping("/test-data")
    public TestDataGenerator.GeneratedData generate(
            @RequestParam(defaultValue = "1") long seed,
            @RequestBody List<TestDataGenerator.GeneratorConfig> configs) {
        return generator.run(configs, seed);
    }

    /** TSTVAL00. */
    @PostMapping("/test-validate")
    public ProgramResult testValidate(
            @RequestBody List<TestValidationProgram.TestCase> cases) {
        return testValidation.run(cases);
    }
}
