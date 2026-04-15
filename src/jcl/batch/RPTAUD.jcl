//******************************************************************
//* JCL Name: RPTAUD
//* Description: Execute System Audit Report Generator
//*   Runs RPTAUD00 to read the audit log and error log,
//*   then produces a formatted audit report dataset.
//* Program: RPTAUD00
//* Input:   AUDITLOG - Production audit log (SHR)
//*          ERRLOG   - Production error log (SHR)
//* Output:  RPTFILE  - Audit report (NEW, FB/132)
//******************************************************************
//RPTAUD00 JOB (ACCT#),'AUDIT REPORT',
//             CLASS=A,MSGCLASS=X,MSGLEVEL=(1,1)
//*
//STEP01   EXEC PGM=RPTAUD00
//STEPLIB  DD   DSN=PROD.LOAD.LIBRARY,DISP=SHR
//AUDITLOG DD   DSN=PROD.AUDIT.LOG,DISP=SHR
//ERRLOG   DD   DSN=PROD.ERROR.LOG,DISP=SHR
//RPTFILE  DD   DSN=PROD.AUDIT.REPORT,
//             DISP=(NEW,CATLG,DELETE),
//             SPACE=(CYL,(10,5),RLSE),
//             DCB=(RECFM=FB,LRECL=132,BLKSIZE=0)
//SYSOUT   DD   SYSOUT=*
//SYSUDUMP DD   SYSOUT=*
//SYSPRINT DD   SYSOUT=*  