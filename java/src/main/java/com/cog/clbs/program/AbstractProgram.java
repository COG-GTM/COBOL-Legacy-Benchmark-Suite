package com.cog.clbs.program;

/**
 * Standard program skeleton.
 *
 * <p>Java equivalent of {@code src/templates/program/standard-program.cbl}.
 * The COBOL template lays out the IDENTIFICATION / ENVIRONMENT / DATA /
 * PROCEDURE division structure with a fixed paragraph flow:
 *
 * <pre>
 *   0000-MAIN:       PERFORM 1000-INITIALIZE
 *                    PERFORM 2000-PROCESS UNTIL END-OF-FILE
 *                    PERFORM 3000-TERMINATE
 *                    GOBACK
 *   1000-INITIALIZE: initialize work areas
 *   2000-PROCESS:    main processing logic
 *   3000-TERMINATE:  MOVE WS-RETURN-CODE TO RETURN-CODE
 *   9000-HANDLE-ERROR: MOVE WS-ERROR (+8) TO WS-RETURN-CODE
 * </pre>
 *
 * <p>Subclasses implement the lifecycle hooks; {@link #run()} drives the
 * standard flow and returns the final RC 0-16 value (the COBOL
 * {@code RETURN-CODE} special register).
 */
public abstract class AbstractProgram {

    /** WS-PROGRAM-NAME: program identifier, max 8 characters on z/OS. */
    private final String programName;

    /** WS-RETURN-CODE: program return code within the RC 0-16 framework. */
    private int returnCode = ReturnCode.SUCCESS.getCode();

    /** WS-END-OF-FILE-SW: 'Y'/'N' switch with 88-levels END-OF-FILE / NOT-END-OF-FILE. */
    private boolean endOfFile = false;

    protected AbstractProgram(String programName) {
        this.programName = programName;
    }

    /**
     * 0000-MAIN: standard control flow. Runs initialize, then process
     * until end-of-file, then terminate. Any unhandled exception routes
     * through {@link #handleError(Exception)} (9000-HANDLE-ERROR).
     *
     * @return the final return code (0-16), as GOBACK would surface it
     */
    public final int run() {
        try {
            initialize();
            while (!endOfFile) {
                execute();
            }
        } catch (Exception e) {
            handleError(e);
        } finally {
            terminate();
        }
        return returnCode;
    }

    /** 1000-INITIALIZE: initialize work areas, open resources. */
    protected abstract void initialize();

    /**
     * 2000-PROCESS: one iteration of the main processing loop.
     * Implementations must eventually call {@link #setEndOfFile()}
     * (SET END-OF-FILE TO TRUE) to end the loop.
     */
    protected abstract void execute();

    /** 3000-TERMINATE: close resources and finalize the return code. */
    protected abstract void terminate();

    /** 9000-HANDLE-ERROR: default error handling sets RC-ERROR (+8). */
    protected void handleError(Exception e) {
        setReturnCode(ReturnCode.ERROR.getCode());
    }

    public String getProgramName() {
        return programName;
    }

    public int getReturnCode() {
        return returnCode;
    }

    /** Sets the return code, never lowering an already-higher severity. */
    protected void setReturnCode(int returnCode) {
        if (returnCode > this.returnCode) {
            this.returnCode = returnCode;
        }
    }

    /** SET END-OF-FILE TO TRUE. */
    protected void setEndOfFile() {
        this.endOfFile = true;
    }

    protected boolean isEndOfFile() {
        return endOfFile;
    }
}
