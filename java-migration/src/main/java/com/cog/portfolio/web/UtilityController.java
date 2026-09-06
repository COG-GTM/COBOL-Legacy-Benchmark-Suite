package com.cog.portfolio.web;

import com.cog.portfolio.service.DataValidationService;
import com.cog.portfolio.service.MaintenanceService;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** src/jcl/utility/UTLMNT.jcl and UTLVAL.jcl. UTLMON.jcl is covered by /actuator/metrics. */
@RestController
public class UtilityController {

    private final MaintenanceService maintenanceService;
    private final DataValidationService dataValidationService;

    public UtilityController(MaintenanceService maintenanceService, DataValidationService dataValidationService) {
        this.maintenanceService = maintenanceService;
        this.dataValidationService = dataValidationService;
    }

    /** UTLMNT control card: ARCHIVE / CLEANUP / REORG / ANALYZE. */
    @PostMapping("/maintenance/{function}")
    public MaintenanceService.MaintenanceResult maintenance(@PathVariable MaintenanceService.Function function) {
        return maintenanceService.run(function);
    }

    /** UTLVAL control cards: any subset of INTEGRITY / XREF / FORMAT / BALANCE (default all). */
    @PostMapping("/validation/run")
    public DataValidationService.ValidationReport validate(
            @RequestParam(name = "type", required = false) Set<DataValidationService.ValidationType> types) {
        Set<DataValidationService.ValidationType> selected = types == null || types.isEmpty()
                ? EnumSet.allOf(DataValidationService.ValidationType.class) : types;
        return dataValidationService.validate(selected);
    }
}
