//******************************************************************
//* JCL Name:    UTLVAL                                            *
//* Description: Execute Data Validation Utility                   *
//* Program:     UTLVAL00                                          *
//*                                                                *
//* Validates data integrity across VSAM files by checking record  *
//* consistency between positions and transactions, verifying       *
//* referential integrity, and detecting orphaned/duplicate keys.   *
//******************************************************************
//UTLVAL00 JOB (ACCT#),'DATA VALIDATION',
//             CLASS=A,MSGCLASS=X,MSGLEVEL=(1,1)
//*
//STEP01   EXEC PGM=UTLVAL00
//STEPLIB  DD   DSN=PROD.LOAD.LIBRARY,DISP=SHR
//* Input: Validation rules and control parameters
//VALCTL   DD   DSN=PROD.VALIDATION.CONTROL,DISP=SHR
//* Input: Position master VSAM file to validate
//POSMSTRE DD   DSN=PROD.POSITION.MASTER,DISP=SHR
//* Input: Transaction history VSAM file to cross-check
//TRANHIST DD   DSN=PROD.TRANSACTION.HISTORY,DISP=SHR
//* Output: Validation error report with details (FB 132)
//ERRRPT   DD   DSN=PROD.VALIDATION.REPORT,
//             DISP=(NEW,CATLG,DELETE),
//             SPACE=(CYL,(10,5),RLSE),
//             DCB=(RECFM=FB,LRECL=132,BLKSIZE=0)
//SYSOUT   DD   SYSOUT=*
//SYSUDUMP DD   SYSOUT=*
//SYSPRINT DD   SYSOUT=*   