//******************************************************************
//* JCL Name: TSTVAL
//* Description: Execute Test Validation Suite
//*   Runs TSTVAL00 to compare actual program outputs against
//*   expected results, producing a pass/fail report with
//*   elapsed time and success percentages.
//* Program: TSTVAL00
//* Input:   TESTCASE - Test case definitions (SHR)
//*          EXPECTED - Expected results file (SHR)
//*          ACTUAL   - Actual results file (SHR)
//* Output:  TESTRPT  - Validation report (NEW, FB/132)
//******************************************************************
//TSTVAL00 JOB (ACCT#),'TEST VALIDATION',
//             CLASS=A,MSGCLASS=X,MSGLEVEL=(1,1)
//*
//STEP01   EXEC PGM=TSTVAL00
//STEPLIB  DD   DSN=TEST.LOAD.LIBRARY,DISP=SHR
//TESTCASE DD   DSN=TEST.CASE.FILE,DISP=SHR
//EXPECTED DD   DSN=TEST.EXPECTED.RESULTS,DISP=SHR
//ACTUAL   DD   DSN=TEST.ACTUAL.RESULTS,DISP=SHR
//TESTRPT  DD   DSN=TEST.VALIDATION.REPORT,
//             DISP=(NEW,CATLG,DELETE),
//             SPACE=(CYL,(10,5),RLSE),
//             DCB=(RECFM=FB,LRECL=132,BLKSIZE=0)
//SYSOUT   DD   SYSOUT=*
//SYSUDUMP DD   SYSOUT=*
//SYSPRINT DD   SYSOUT=*  