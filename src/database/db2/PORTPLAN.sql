--=====================================================================
-- SQL File: PORTPLAN.sql
-- Description: DB2 Application Plan for Portfolio System
--   Binds the PORTPLAN plan with package list *.PORTPKG.*,
--   using cursor stability (CS) isolation, use-time acquire,
--   commit-time release, and EXPLAIN enabled.
-- Used By: PORTDFN.csd (DB2ENTRY PORTDB2 references this plan)
--=====================================================================
-- DB2 Plan Definition for Portfolio System
BIND PLAN PORTPLAN
     PKLIST(*.PORTPKG.*)
     ACTION(REPLACE)
     RETAIN
     VALIDATE(RUN)
     ISOLATION(CS)
     ACQUIRE(USE)
     RELEASE(COMMIT)
     EXPLAIN(YES);  