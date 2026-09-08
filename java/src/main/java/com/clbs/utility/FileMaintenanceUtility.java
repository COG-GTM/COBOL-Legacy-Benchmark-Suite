package com.clbs.utility;

import com.clbs.common.Inputs;
import com.clbs.common.ProgramResult;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * UTLMNT00 — file maintenance utility.
 *
 * <p>The COBOL source defines the CONTROL-FILE layout, the ARCHIVE/CLEANUP/REORG/ANALYZE dispatch
 * and the error handler (abort once WS-ERROR-COUNT exceeds 100). Every leaf paragraph
 * (2210-2230, 2310-2330, 2410-2430, 2510-2520) is PERFORMed but never defined, so each function
 * here is recognised and counted without inventing archive or VSAM reorganisation behaviour.
 */
@Service
public class FileMaintenanceUtility {

    public static final String PROGRAM = "UTLMNT00";
    public static final int ERROR_LIMIT = 100;
    public static final String ERR_INVALID_FUNCTION = "INVALID FUNCTION SPECIFIED";

    public static final String ARCHIVE = "ARCHIVE";
    public static final String CLEANUP = "CLEANUP";
    public static final String REORG = "REORG";
    public static final String ANALYZE = "ANALYZE";

    /** CONTROL-RECORD. */
    public record ControlRecord(String function, String fileName, String parameters) {
    }

    /** 0000-MAIN. */
    public ProgramResult run(List<ControlRecord> controls) {
        ProgramResult result = new ProgramResult(PROGRAM);
        long read = 0;
        long errors = 0;

        for (ControlRecord control : Inputs.records(controls)) {
            read++;
            String function = control.function() == null ? "" : control.function().trim();
            switch (function) {
                case ARCHIVE, CLEANUP, REORG, ANALYZE ->
                        result.display(function + " requested for " + control.fileName()
                                + " (no operation defined in UTLMNT00)");
                default -> {
                    errors++;
                    result.display(ERR_INVALID_FUNCTION);
                }
            }
            if (errors > ERROR_LIMIT) {
                result.setReturnCode(12);
                break;
            }
        }

        result.count("read", read).count("errors", errors);
        return result;
    }
}
