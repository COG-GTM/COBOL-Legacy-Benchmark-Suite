//******************************************************************
//* JCL Name: RPTSTA
//* Description: Execute System Statistics Report Generator
//*   Runs RPTSTA00 to read DB2 and batch statistics files,
//*   then produces a system performance statistics report.
//* Program: RPTSTA00
//* Input:   DB2STATS - DB2 statistics file (SHR)
//*          BCHSTATS - Batch statistics file (SHR)
//* Output:  RPTFILE  - System stats report (NEW, FB/132)
//******************************************************************
//RPTSTA00 JOB (ACCT#),'SYSTEM STATS RPT',
//             CLASS=A,MSGCLASS=X,MSGLEVEL=(1,1)
//*
//STEP01   EXEC PGM=RPTSTA00
//STEPLIB  DD   DSN=PROD.LOAD.LIBRARY,DISP=SHR
//DB2STATS DD   DSN=PROD.DB2.STATISTICS,DISP=SHR
//BCHSTATS DD   DSN=PROD.BATCH.STATISTICS,DISP=SHR
//RPTFILE  DD   DSN=PROD.SYSTEM.STATS.REPORT,
//             DISP=(NEW,CATLG,DELETE),
//             SPACE=(CYL,(10,5),RLSE),
//             DCB=(RECFM=FB,LRECL=132,BLKSIZE=0)
//SYSOUT   DD   SYSOUT=*
//SYSUDUMP DD   SYSOUT=*
//SYSPRINT DD   SYSOUT=*  