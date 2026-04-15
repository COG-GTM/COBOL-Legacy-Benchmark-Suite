//******************************************************************
//* JCL Name: UTLMON
//* Description: Execute System Monitoring Utility
//*   Runs UTLMON00 to continuously collect CPU, memory, DASD,
//*   and DB2 metrics until 11 PM, logging status and generating
//*   alerts when thresholds are exceeded.
//* Program: UTLMON00
//* Input:   MONCFG   - Monitor configuration/thresholds (SHR)
//*          DB2STATS - DB2 statistics file (SHR)
//* Output:  MONLOG   - Monitor log (NEW, FB/132)
//*          ALERTS   - Alert records (NEW, FB/132)
//******************************************************************
//UTLMON00 JOB (ACCT#),'SYSTEM MONITOR',
//             CLASS=A,MSGCLASS=X,MSGLEVEL=(1,1)
//*
//STEP01   EXEC PGM=UTLMON00
//STEPLIB  DD   DSN=PROD.LOAD.LIBRARY,DISP=SHR
//MONCFG   DD   DSN=PROD.MONITOR.CONFIG,DISP=SHR
//MONLOG   DD   DSN=PROD.MONITOR.LOG,
//             DISP=(NEW,CATLG,DELETE),
//             SPACE=(CYL,(50,20),RLSE),
//             DCB=(RECFM=FB,LRECL=132,BLKSIZE=0)
//ALERTS   DD   DSN=PROD.MONITOR.ALERTS,
//             DISP=(NEW,CATLG,DELETE),
//             SPACE=(CYL,(10,5),RLSE),
//             DCB=(RECFM=FB,LRECL=132,BLKSIZE=0)
//DB2STATS DD   DSN=PROD.DB2.STATISTICS,DISP=SHR
//SYSOUT   DD   SYSOUT=*
//SYSUDUMP DD   SYSOUT=*
//SYSPRINT DD   SYSOUT=*  