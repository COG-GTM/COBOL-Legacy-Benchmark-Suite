//******************************************************************
//* JCL Name:    RPTSTA                                            *
//* Description: Generate System Statistics Report                 *
//* Program:     RPTSTA00                                          *
//*                                                                *
//* Reads DB2 and batch processing statistics to produce a system  *
//* performance report covering throughput, error rates, resource   *
//* utilization, and processing times across all batch programs.   *
//******************************************************************
//RPTSTA00 JOB (ACCT#),'SYSTEM STATS RPT',
//             CLASS=A,MSGCLASS=X,MSGLEVEL=(1,1)
//*
//STEP01   EXEC PGM=RPTSTA00
//STEPLIB  DD   DSN=PROD.LOAD.LIBRARY,DISP=SHR
//* Input: DB2 performance and usage statistics
//DB2STATS DD   DSN=PROD.DB2.STATISTICS,DISP=SHR
//* Input: Batch program processing statistics
//BCHSTATS DD   DSN=PROD.BATCH.STATISTICS,DISP=SHR
//* Output: System statistics report (FB 132, allocated 10+5 cyl)
//RPTFILE  DD   DSN=PROD.SYSTEM.STATS.REPORT,
//             DISP=(NEW,CATLG,DELETE),
//             SPACE=(CYL,(10,5),RLSE),
//             DCB=(RECFM=FB,LRECL=132,BLKSIZE=0)
//SYSOUT   DD   SYSOUT=*
//SYSUDUMP DD   SYSOUT=*
//SYSPRINT DD   SYSOUT=*   