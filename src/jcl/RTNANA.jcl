//******************************************************************
//* JCL Name:    RTNANA                                            *
//* Description: Execute Return Code Analysis Program              *
//* Program:     RTNANA00                                          *
//*                                                                *
//* Reads the RTNCODES DB2 table to analyze return code patterns   *
//* across batch programs and generates a formatted report showing  *
//* code distributions, trends, and anomalies.                     *
//******************************************************************
//RTNANA00 JOB (ACCT),'RETURN CODE ANALYSIS',
//             CLASS=A,MSGCLASS=X,MSGLEVEL=(1,1)
//*
//RTNANA   EXEC PGM=RTNANA00
//STEPLIB  DD DSN=PROD.LOAD.LIBRARY,DISP=SHR
//* Analysis report output (FBA 133 for ASA carriage control)
//RPTFILE  DD SYSOUT=*,
//            DCB=(RECFM=FBA,LRECL=133,BLKSIZE=0)
//SYSOUT   DD SYSOUT=*
//SYSUDUMP DD SYSOUT=*
//SYSPRINT DD SYSOUT=*   