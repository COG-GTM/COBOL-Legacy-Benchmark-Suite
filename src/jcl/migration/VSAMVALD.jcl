//*================================================================*
//* JCL Name: VSAMVALD
//* Description: VSAM Validation for Date Field Migration
//* Purpose: Validate data integrity after date field migration
//*          from 8-digit (YYYYMMDD) to 10-digit (YYYY-MM-DD)
//* Author: Date Migration Team
//* Date Written: 2024-12-18
//*
//* VALIDATION CHECKS:
//* 1. Record counts match pre-migration counts
//* 2. Date format is YYYY-MM-DD (contains hyphens)
//* 3. Key integrity verified
//* 4. Sample records validated
//*================================================================*
//VSAMVALD JOB (ACCT),'VSAM VALIDATE',CLASS=A,MSGCLASS=X,
//         NOTIFY=&SYSUID,REGION=0M
//*
//*================================================================*
//* STEP 1: COUNT RECORDS IN TRANSACTION HISTORY
//*================================================================*
//CNTTRN   EXEC PGM=IDCAMS
//SYSPRINT DD SYSOUT=*
//INFILE   DD DSN=PORTFOLIO.TRANHIST.VSAM,DISP=SHR
//SYSIN    DD *
  PRINT INFILE(INFILE) COUNT(10) CHARACTER
  LISTCAT ENTRIES(PORTFOLIO.TRANHIST.VSAM) ALL
/*
//*
//*================================================================*
//* STEP 2: COUNT RECORDS IN POSITION HISTORY
//*================================================================*
//CNTPOS   EXEC PGM=IDCAMS
//SYSPRINT DD SYSOUT=*
//INFILE   DD DSN=PORTFOLIO.POSHIST.VSAM,DISP=SHR
//SYSIN    DD *
  PRINT INFILE(INFILE) COUNT(10) CHARACTER
  LISTCAT ENTRIES(PORTFOLIO.POSHIST.VSAM) ALL
/*
//*
//*================================================================*
//* STEP 3: COUNT RECORDS IN PORTFOLIO MASTER
//*================================================================*
//CNTPORT  EXEC PGM=IDCAMS
//SYSPRINT DD SYSOUT=*
//INFILE   DD DSN=PORTFOLIO.PORTMSTR.VSAM,DISP=SHR
//SYSIN    DD *
  PRINT INFILE(INFILE) COUNT(10) CHARACTER
  LISTCAT ENTRIES(PORTFOLIO.PORTMSTR.VSAM) ALL
/*
//*
//*================================================================*
//* STEP 4: COUNT RECORDS IN BATCH CONTROL
//*================================================================*
//CNTBCH   EXEC PGM=IDCAMS
//SYSPRINT DD SYSOUT=*
//INFILE   DD DSN=PORTFOLIO.BCHCTL.VSAM,DISP=SHR
//SYSIN    DD *
  PRINT INFILE(INFILE) COUNT(10) CHARACTER
  LISTCAT ENTRIES(PORTFOLIO.BCHCTL.VSAM) ALL
/*
//*
//*================================================================*
//* STEP 5: VERIFY CHECKPOINT FILE IS EMPTY (Expected after migration)
//*================================================================*
//CNTCKP   EXEC PGM=IDCAMS
//SYSPRINT DD SYSOUT=*
//INFILE   DD DSN=PORTFOLIO.CKPRST.VSAM,DISP=SHR
//SYSIN    DD *
  LISTCAT ENTRIES(PORTFOLIO.CKPRST.VSAM) ALL
/*
//*
//*================================================================*
//* STEP 6: RUN VALIDATION REPORT PROGRAM
//* Validates date format in sample records
//*================================================================*
//VALRPT   EXEC PGM=VALDRPT
//STEPLIB  DD DSN=PORTFOLIO.LOADLIB,DISP=SHR
//TRANHIST DD DSN=PORTFOLIO.TRANHIST.VSAM,DISP=SHR
//POSHIST  DD DSN=PORTFOLIO.POSHIST.VSAM,DISP=SHR
//PORTMSTR DD DSN=PORTFOLIO.PORTMSTR.VSAM,DISP=SHR
//BCHCTL   DD DSN=PORTFOLIO.BCHCTL.VSAM,DISP=SHR
//REPORT   DD SYSOUT=*
//SYSOUT   DD SYSOUT=*
//*
//*================================================================*
//* STEP 7: RUN TEST BATCH JOB TO VERIFY PROCESSING
//*================================================================*
//TESTBCH  EXEC PGM=RPTPOS00
//STEPLIB  DD DSN=PORTFOLIO.LOADLIB,DISP=SHR
//POSHIST  DD DSN=PORTFOLIO.POSHIST.VSAM,DISP=SHR
//REPORT   DD SYSOUT=*
//SYSOUT   DD SYSOUT=*
//*================================================================*
//* END OF JCL
//*================================================================*
