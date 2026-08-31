package com.clbs.posval.error;

/**
 * {@code ERR-MESSAGE} of {@code src/copybook/common/ERRHAND.cpy}, the record {@code ERRPROC}
 * writes to the error log.
 *
 * @param programName {@code ERR-PROGRAM PIC X(8)}
 * @param category {@code ERR-CATEGORY PIC X(2)}
 * @param code {@code ERR-CODE PIC X(4)} — the VSAM file status, where one applies
 * @param severity {@code ERR-SEVERITY PIC S9(4) COMP}, one of {@link Severity}
 * @param text {@code ERR-TEXT PIC X(80)}
 * @param details {@code ERR-DETAILS PIC X(256)}
 */
public record ErrorRecord(
        String programName, String category, String code, int severity, String text, String details) {

    /** {@code ERR-CAT-VSAM PIC X(2) VALUE 'VS'}. */
    public static final String CATEGORY_VSAM = "VS";
    /** {@code ERR-CAT-VALID PIC X(2) VALUE 'VL'}. */
    public static final String CATEGORY_VALIDATION = "VL";
    /** {@code ERR-CAT-PROC PIC X(2) VALUE 'PR'}. */
    public static final String CATEGORY_PROCESSING = "PR";
    /** {@code ERR-CAT-SYSTEM PIC X(2) VALUE 'SY'}. */
    public static final String CATEGORY_SYSTEM = "SY";

    /** The {@code ERR-RETURN-CODES} of the ERRHAND copybook. */
    public static final class Severity {
        /** {@code ERR-SUCCESS VALUE +0}. */
        public static final int SUCCESS = 0;
        /** {@code ERR-WARNING VALUE +4}. */
        public static final int WARNING = 4;
        /** {@code ERR-ERROR VALUE +8}. */
        public static final int ERROR = 8;
        /** {@code ERR-SEVERE VALUE +12}. */
        public static final int SEVERE = 12;
        /** {@code ERR-TERMINAL VALUE +16}. */
        public static final int TERMINAL = 16;

        private Severity() {}
    }

    /**
     * The record {@code PORTTRAN 9000-ERROR-ROUTINE} builds: category {@code 'PR'}, program
     * {@code 'PORTTRAN'} and the {@code ERR-TEXT} set by the failing paragraph. The paragraph sets
     * neither {@code ERR-CODE} nor {@code ERR-SEVERITY}, so both stay at their initial values.
     */
    public static ErrorRecord processing(String programName, String text) {
        return new ErrorRecord(programName, CATEGORY_PROCESSING, "", Severity.SUCCESS, text, "");
    }
}
