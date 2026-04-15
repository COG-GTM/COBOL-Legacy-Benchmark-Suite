//******************************************************************
//* JCL Name:    TSTVAL                                            *
//* Description: Execute Test Validation Suite                     *
//* Program:     TSTVAL00                                          *
//*                                                                *
//* Compares actual program output against expected results to     *
//* verify correctness of translated or modified COBOL programs.   *
//* Produces a pass/fail validation report with detailed diffs.    *
//******************************************************************
//TSTVAL00 JOB (ACCT#),'TEST VALIDATION',
//             CLASS=A,MSGCLASS=X,MSGLEVEL=(1,1)
//*
//STEP01   EXEC PGM=TSTVAL00
//STEPLIB  DD   DSN=TEST.LOAD.LIBRARY,DISP=SHR
//* Input: Test case definitions (categories and parameters)
//TESTCASE DD   DSN=TEST.CASE.FILE,DISP=SHR
//* Input: Expected output from baseline COBOL execution
//EXPECTED DD   DSN=TEST.EXPECTED.RESULTS,DISP=SHR
//* Input: Actual output from translated/modified programs
//ACTUAL   DD   DSN=TEST.ACTUAL.RESULTS,DISP=SHR
//* Output: Validation report with pass/fail and diffs (FB 132)
//TESTRPT  DD   DSN=TEST.VALIDATION.REPORT,
//             DISP=(NEW,CATLG,DELETE),
//             SPACE=(CYL,(10,5),RLSE),
//             DCB=(RECFM=FB,LRECL=132,BLKSIZE=0)
//SYSOUT   DD   SYSOUT=*
//SYSUDUMP DD   SYSOUT=*
//SYSPRINT DD   SYSOUT=*   