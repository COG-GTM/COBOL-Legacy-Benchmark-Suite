--=====================================================================
-- Plan Name:    PORTPLAN
-- Description:  DB2 Plan Definition for Portfolio System
--
-- Binds all packages under the PORTPKG collection into a single
-- plan used by all portfolio COBOL programs at runtime.
--
-- Key settings:
--   ACTION(REPLACE)   - Replaces existing plan on rebind
--   RETAIN            - Keeps existing plan if rebind fails
--   VALIDATE(RUN)     - Defers auth/existence checks to runtime
--   ISOLATION(CS)     - Cursor stability (read committed)
--   ACQUIRE(USE)      - Acquires locks only when objects are used
--   RELEASE(COMMIT)   - Releases locks at each COMMIT point
--   EXPLAIN(YES)      - Generates EXPLAIN output for tuning
--=====================================================================
BIND PLAN PORTPLAN
     PKLIST(*.PORTPKG.*)
     ACTION(REPLACE)
     RETAIN
     VALIDATE(RUN)
     ISOLATION(CS)
     ACQUIRE(USE)
     RELEASE(COMMIT)
     EXPLAIN(YES);   