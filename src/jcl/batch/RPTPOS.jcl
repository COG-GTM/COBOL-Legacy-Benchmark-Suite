//******************************************************************
//* JCL Name: RPTPOS
//* Description: Execute Daily Position Report Generator
//*   Runs RPTPOS00 to read position master and transaction
//*   history files, then produces a daily position report.
//* Program: RPTPOS00
//* Input:   POSMSTRE - Position master file (SHR)
//*          TRANHIST - Transaction history file (SHR)
//* Output:  RPTFILE  - Daily position report (NEW, FB/132)
//******************************************************************
//RPTPOS00 JOB (ACCT#),'DAILY POSITION RPT',
//             CLASS=A,MSGCLASS=X,MSGLEVEL=(1,1)
//*
//STEP01   EXEC PGM=RPTPOS00
//STEPLIB  DD   DSN=PROD.LOAD.LIBRARY,DISP=SHR
//POSMSTRE DD   DSN=PROD.POSITION.MASTER,DISP=SHR
//TRANHIST DD   DSN=PROD.TRANSACTION.HISTORY,DISP=SHR
//RPTFILE  DD   DSN=PROD.DAILY.POSITION.REPORT,
//             DISP=(NEW,CATLG,DELETE),
//             SPACE=(CYL,(10,5),RLSE),
//             DCB=(RECFM=FB,LRECL=132,BLKSIZE=0)
//SYSOUT   DD   SYSOUT=*
//SYSUDUMP DD   SYSOUT=*
//SYSPRINT DD   SYSOUT=*  