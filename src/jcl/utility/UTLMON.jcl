//******************************************************************
//* JCL Name:    UTLMON                                            *
//* Description: Execute System Monitoring Utility                 *
//* Program:     UTLMON00                                          *
//*                                                                *
//* Monitors system health by checking DB2 statistics, VSAM file   *
//* utilization, batch completion status, and threshold violations. *
//* Generates alerts for any metrics exceeding configured limits.   *
//******************************************************************
//UTLMON00 JOB (ACCT#),'SYSTEM MONITOR',
//             CLASS=A,MSGCLASS=X,MSGLEVEL=(1,1)
//*
//STEP01   EXEC PGM=UTLMON00
//STEPLIB  DD   DSN=PROD.LOAD.LIBRARY,DISP=SHR
//* Input: Monitoring thresholds and check definitions
//MONCFG   DD   DSN=PROD.MONITOR.CONFIG,DISP=SHR
//* Output: Detailed monitoring log (FB 132, 50+20 cyl)
//MONLOG   DD   DSN=PROD.MONITOR.LOG,
//             DISP=(NEW,CATLG,DELETE),
//             SPACE=(CYL,(50,20),RLSE),
//             DCB=(RECFM=FB,LRECL=132,BLKSIZE=0)
//* Output: Alert records for threshold violations (FB 132)
//ALERTS   DD   DSN=PROD.MONITOR.ALERTS,
//             DISP=(NEW,CATLG,DELETE),
//             SPACE=(CYL,(10,5),RLSE),
//             DCB=(RECFM=FB,LRECL=132,BLKSIZE=0)
//* Input: DB2 performance statistics for analysis
//DB2STATS DD   DSN=PROD.DB2.STATISTICS,DISP=SHR
//SYSOUT   DD   SYSOUT=*
//SYSUDUMP DD   SYSOUT=*
//SYSPRINT DD   SYSOUT=*   