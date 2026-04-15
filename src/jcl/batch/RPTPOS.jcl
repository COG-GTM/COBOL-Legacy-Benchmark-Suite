//******************************************************************
//* JCL Name:    RPTPOS                                            *
//* Description: Generate Daily Position Report                    *
//* Program:     RPTPOS00                                          *
//*                                                                *
//* Reads the position master VSAM file and transaction history    *
//* to produce a daily portfolio valuation and position summary    *
//* report with totals by portfolio, currency, and security.       *
//******************************************************************
//RPTPOS00 JOB (ACCT#),'DAILY POSITION RPT',
//             CLASS=A,MSGCLASS=X,MSGLEVEL=(1,1)
//*
//STEP01   EXEC PGM=RPTPOS00
//STEPLIB  DD   DSN=PROD.LOAD.LIBRARY,DISP=SHR
//* Input: VSAM KSDS containing current portfolio positions
//POSMSTRE DD   DSN=PROD.POSITION.MASTER,DISP=SHR
//* Input: VSAM KSDS containing transaction records
//TRANHIST DD   DSN=PROD.TRANSACTION.HISTORY,DISP=SHR
//* Output: Daily position report (FB 132, allocated 10+5 cyl)
//RPTFILE  DD   DSN=PROD.DAILY.POSITION.REPORT,
//             DISP=(NEW,CATLG,DELETE),
//             SPACE=(CYL,(10,5),RLSE),
//             DCB=(RECFM=FB,LRECL=132,BLKSIZE=0)
//SYSOUT   DD   SYSOUT=*
//SYSUDUMP DD   SYSOUT=*
//SYSPRINT DD   SYSOUT=*   