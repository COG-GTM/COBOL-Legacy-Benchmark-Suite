//******************************************************************
//* JCL Name:    PORTDEL                                           *
//* Description: Execute Portfolio Deletion Program                *
//* Program:     PORTDEL                                           *
//* Author: [Author name]                                          *
//* Date Written: 2024-03-20                                       *
//*                                                                *
//* Reads portfolio IDs from the delete request file, validates    *
//* each deletion, logically deletes records from the Portfolio     *
//* Master VSAM file, and writes before-images to the audit file.  *
//******************************************************************
//PORTDEL   JOB (ACCT),'DELETE PORTFOLIO',
//          CLASS=A,MSGCLASS=X,NOTIFY=&SYSUID
//*
//STEP1    EXEC PGM=PORTDEL
//STEPLIB   DD DSN=YOUR.LOADLIB,DISP=SHR
//* Portfolio Master VSAM KSDS file (input/output)
//PORTFILE  DD DSN=PORTFOLIO.MASTER.FILE,DISP=SHR
//* Input file listing portfolio IDs to delete
//DELEFILE  DD DSN=PORTFOLIO.DELETE.FILE,DISP=OLD
//* Audit trail file (append mode) for deletion records
//AUDFILE   DD DSN=PORTFOLIO.AUDIT.FILE,DISP=MOD
//SYSOUT    DD SYSOUT=*
//SYSPRINT  DD SYSOUT=*
//SYSUDUMP  DD SYSOUT=*   