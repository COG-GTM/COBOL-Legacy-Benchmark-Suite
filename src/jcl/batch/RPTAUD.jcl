//******************************************************************
//* JCL Name:    RPTAUD                                            *
//* Description: Generate System Audit Report                      *
//* Program:     RPTAUD00                                          *
//*                                                                *
//* Reads the audit log and error log files, then produces a       *
//* formatted audit report covering security events, user actions,  *
//* system errors, and compliance-relevant activities.              *
//******************************************************************
//RPTAUD00 JOB (ACCT#),'AUDIT REPORT',
//             CLASS=A,MSGCLASS=X,MSGLEVEL=(1,1)
//*
//STEP01   EXEC PGM=RPTAUD00
//STEPLIB  DD   DSN=PROD.LOAD.LIBRARY,DISP=SHR
//* Input: Audit log containing all recorded system events
//AUDITLOG DD   DSN=PROD.AUDIT.LOG,DISP=SHR
//* Input: Error log for cross-referencing error events
//ERRLOG   DD   DSN=PROD.ERROR.LOG,DISP=SHR
//* Output: Formatted audit report (FB 132, allocated 10+5 cyl)
//RPTFILE  DD   DSN=PROD.AUDIT.REPORT,
//             DISP=(NEW,CATLG,DELETE),
//             SPACE=(CYL,(10,5),RLSE),
//             DCB=(RECFM=FB,LRECL=132,BLKSIZE=0)
//SYSOUT   DD   SYSOUT=*
//SYSUDUMP DD   SYSOUT=*
//SYSPRINT DD   SYSOUT=*   