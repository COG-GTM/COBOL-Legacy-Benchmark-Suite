//******************************************************************
//* JCL Name: UTLVAL
//* Description: Execute Data Validation Utility
//*   Runs UTLVAL00 to perform integrity, cross-reference,
//*   format, and balance checks on position and transaction
//*   files, writing discrepancies to an error report.
//* Program: UTLVAL00
//* Input:   VALCTL   - Validation control records (SHR)
//*          POSMSTRE - Position master file (SHR)
//*          TRANHIST - Transaction history file (SHR)
//* Output:  ERRRPT   - Validation error report (NEW, FB/132)
//******************************************************************
//UTLVAL00 JOB (ACCT#),'DATA VALIDATION',
//             CLASS=A,MSGCLASS=X,MSGLEVEL=(1,1)
//*
//STEP01   EXEC PGM=UTLVAL00
//STEPLIB  DD   DSN=PROD.LOAD.LIBRARY,DISP=SHR
//VALCTL   DD   DSN=PROD.VALIDATION.CONTROL,DISP=SHR
//POSMSTRE DD   DSN=PROD.POSITION.MASTER,DISP=SHR
//TRANHIST DD   DSN=PROD.TRANSACTION.HISTORY,DISP=SHR
//ERRRPT   DD   DSN=PROD.VALIDATION.REPORT,
//             DISP=(NEW,CATLG,DELETE),
//             SPACE=(CYL,(10,5),RLSE),
//             DCB=(RECFM=FB,LRECL=132,BLKSIZE=0)
//SYSOUT   DD   SYSOUT=*
//SYSUDUMP DD   SYSOUT=*
//SYSPRINT DD   SYSOUT=*  